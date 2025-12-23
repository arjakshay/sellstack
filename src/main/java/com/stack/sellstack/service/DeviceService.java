package com.stack.sellstack.service;

import com.stack.sellstack.exception.BusinessException;
import com.stack.sellstack.model.entity.Seller;
import com.stack.sellstack.model.entity.Session;
import com.stack.sellstack.model.enums.DeviceType;
import com.stack.sellstack.repository.SellerRepository;
import com.stack.sellstack.repository.SessionRepository;
import com.stack.sellstack.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceService {

    private final SessionRepository sessionRepository;
    private final SellerRepository sellerRepository;
    private final SecurityAuditService securityAuditService;

    @Value("${session.max.devices:5}")
    private int maxDevicesPerUser;

    @Value("${session.refresh.token.validity.days:30}")
    private int refreshTokenValidityDays;

    @Value("${session.access.token.validity.minutes:15}")
    private int accessTokenValidityMinutes;

    /**
     * Register a new device and create device fingerprint
     */
    public String registerDevice(HttpServletRequest request) {
        return SecurityUtils.generateDeviceFingerprint(request);
    }

    /**
     * Create a new session for the user
     */
    @Transactional
    public Session createSession(UUID sellerId, String deviceId, String refreshToken,
                                 HttpServletRequest request) {

        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new BusinessException("Seller not found"));

        // Check active sessions count
        List<Session> activeSessions = sessionRepository.findBySellerIdAndIsActive(sellerId, true);

        if (activeSessions.size() >= maxDevicesPerUser) {
            // Revoke oldest session
            Session oldestSession = activeSessions.stream()
                    .min((s1, s2) -> s1.getLastAccessedAt().compareTo(s2.getLastAccessedAt()))
                    .orElseThrow(() -> new BusinessException("Failed to manage sessions"));

            revokeSession(oldestSession, "DEVICE_LIMIT_EXCEEDED");

            securityAuditService.logSessionRevoked(
                    sellerId,
                    oldestSession.getDeviceId(),
                    "DEVICE_LIMIT_EXCEEDED",
                    SecurityUtils.getClientIP(request)
            );
        }

        // Detect device information
        DeviceInfo deviceInfo = detectDeviceInfo(request);

        // Create session
        Session session = Session.builder()
                .seller(seller)
                .sessionToken(generateSessionToken())
                .refreshTokenHash(SecurityUtils.hashString(refreshToken))
                .deviceId(deviceId)
                .deviceName(deviceInfo.deviceName())
                .deviceType(deviceInfo.deviceType())
                .osName(deviceInfo.osName())
                .browserName(deviceInfo.browserName())
                .ipAddress(SecurityUtils.getClientIP(request))
                .countryCode(detectCountryCode(request))
                .regionName(detectRegionName(request))
                .cityName(detectCityName(request))
                .isActive(true)
                .isSuspicious(false)
                .lastAccessedAt(Instant.now())
                .expiresAt(Instant.now().plus(refreshTokenValidityDays, ChronoUnit.DAYS))
                .build();

        Session savedSession = sessionRepository.save(session);

        securityAuditService.logSessionCreated(
                sellerId,
                deviceId,
                deviceInfo.deviceType().toString(),
                SecurityUtils.getClientIP(request)
        );

        log.info("Session created for seller: {}, device: {}", sellerId, deviceId);
        return savedSession;
    }

    /**
     * Check if session is valid
     */
    @Transactional
    public boolean isSessionValid(String username, String deviceId, String refreshToken) {
        Optional<Seller> sellerOpt = sellerRepository.findByEmailOrPhone(username, username);

        if (sellerOpt.isEmpty()) {
            return false;
        }

        Seller seller = sellerOpt.get();

        // Find active session
        Optional<Session> sessionOpt = sessionRepository.findActiveSession(
                seller.getId(),
                deviceId,
                Instant.now()
        );

        if (sessionOpt.isEmpty()) {
            return false;
        }

        Session session = sessionOpt.get();

        // Verify refresh token hash
        String refreshTokenHash = SecurityUtils.hashString(refreshToken);
        if (!session.getRefreshTokenHash().equals(refreshTokenHash)) {
            markSessionAsSuspicious(session);
            return false;
        }

        // Update last accessed time
        session.setLastAccessedAt(Instant.now());
        sessionRepository.save(session);

        return true;
    }

    /**
     * Invalidate specific session
     */
    @Transactional
    public void invalidateSession(String username, String deviceId) {
        Optional<Seller> sellerOpt = sellerRepository.findByEmailOrPhone(username, username);

        if (sellerOpt.isPresent()) {
            sessionRepository.invalidateSession(
                    sellerOpt.get().getId(),
                    deviceId,
                    Instant.now()
            );

            log.info("Session invalidated for user: {}, device: {}", username, deviceId);
        }
    }

    /**
     * Invalidate all sessions for a user
     */
    @Transactional
    public void invalidateAllSessions(String username) {
        Optional<Seller> sellerOpt = sellerRepository.findByEmailOrPhone(username, username);

        if (sellerOpt.isPresent()) {
            sessionRepository.invalidateAllSessions(
                    sellerOpt.get().getId(),
                    Instant.now()
            );

            log.info("All sessions invalidated for user: {}", username);
        }
    }

    /**
     * Get active sessions for a user
     */
    public List<Session> getActiveSessions(String username) {
        Optional<Seller> sellerOpt = sellerRepository.findByEmailOrPhone(username, username);

        if (sellerOpt.isPresent()) {
            return sessionRepository.findBySellerIdAndIsActive(sellerOpt.get().getId(), true);
        }

        return List.of();
    }

    /**
     * Revoke a specific session
     */
    @Transactional
    public void revokeSession(Session session, String reason) {
        session.setIsActive(false);
        session.setRevokedAt(Instant.now());
        sessionRepository.save(session);

        log.info("Session revoked: ID {}, Reason: {}", session.getId(), reason);
    }

    /**
     * Detect device information from request
     */
    private DeviceInfo detectDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");

        if (userAgent == null) {
            return new DeviceInfo("Unknown", DeviceType.UNKNOWN, "Unknown", "Unknown");
        }

        String userAgentLower = userAgent.toLowerCase();

        // Detect OS
        String osName = "Unknown";
        if (userAgentLower.contains("windows")) {
            osName = "Windows";
        } else if (userAgentLower.contains("mac os")) {
            osName = "macOS";
        } else if (userAgentLower.contains("linux")) {
            osName = "Linux";
        } else if (userAgentLower.contains("android")) {
            osName = "Android";
        } else if (userAgentLower.contains("iphone") || userAgentLower.contains("ipad")) {
            osName = "iOS";
        }

        // Detect browser
        String browserName = "Unknown";
        if (userAgentLower.contains("chrome") && !userAgentLower.contains("edg")) {
            browserName = "Chrome";
        } else if (userAgentLower.contains("firefox")) {
            browserName = "Firefox";
        } else if (userAgentLower.contains("safari") && !userAgentLower.contains("chrome")) {
            browserName = "Safari";
        } else if (userAgentLower.contains("edg")) {
            browserName = "Edge";
        }

        // Detect device type
        DeviceType deviceType = DeviceType.DESKTOP;
        if (userAgentLower.contains("mobile")) {
            deviceType = DeviceType.MOBILE;
        } else if (userAgentLower.contains("tablet") || userAgentLower.contains("ipad")) {
            deviceType = DeviceType.TABLET;
        }

        // Generate device name
        String deviceName = String.format("%s on %s", browserName, osName);

        return new DeviceInfo(deviceName, deviceType, osName, browserName);
    }

    /**
     * Mark session as suspicious
     */
    private void markSessionAsSuspicious(Session session) {
        session.setIsSuspicious(true);
        session.setRevokedAt(Instant.now());
        session.setIsActive(false);
        sessionRepository.save(session);

        log.warn("Session marked as suspicious: ID {}, Reason: {}", session.getId(), "INVALID_REFRESH_TOKEN");
    }

    /**
     * Generate unique session token
     */
    private String generateSessionToken() {
        try {
            String rawToken = UUID.randomUUID().toString() + Instant.now().toEpochMilli();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to generate session token", e);
            return UUID.randomUUID().toString();
        }
    }

    /**
     * Detect country code from IP (simplified - in production use GeoIP service)
     */
    private String detectCountryCode(HttpServletRequest request) {
        // TODO: Integrate with GeoIP service like MaxMind
        // For now, return empty
        return null;
    }

    private String detectRegionName(HttpServletRequest request) {
        // TODO: Integrate with GeoIP service
        return null;
    }

    private String detectCityName(HttpServletRequest request) {
        // TODO: Integrate with GeoIP service
        return null;
    }

    /**
     * Record representing device information
     */
    private record DeviceInfo(
            String deviceName,
            DeviceType deviceType,
            String osName,
            String browserName
    ) {}
}