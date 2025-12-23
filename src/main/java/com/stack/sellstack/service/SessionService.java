package com.stack.sellstack.service;

import com.stack.sellstack.exception.BusinessException;
import com.stack.sellstack.model.entity.Seller;
import com.stack.sellstack.model.entity.Session;
import com.stack.sellstack.model.enums.DeviceType;
import com.stack.sellstack.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final SessionRepository sessionRepository;

    /**
     * Create a new session
     */
    @Transactional
    public Session createSession(Seller seller, String sessionToken, String refreshTokenHash,
                                 String deviceId, String deviceName, DeviceType deviceType,
                                 String osName, String browserName, String ipAddress,
                                 int validityDays) {

        Session session = Session.builder()
                .seller(seller)  // Set the Seller object, not sellerId
                .sessionToken(sessionToken)
                .refreshTokenHash(refreshTokenHash)
                .deviceId(deviceId)
                .deviceName(deviceName)
                .deviceType(deviceType) // This should be a String if your entity expects String
                .osName(osName)
                .browserName(browserName)
                .ipAddress(ipAddress)
                .isActive(true)
                .isSuspicious(false)
                .lastAccessedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(validityDays * 24 * 3600L))
                .build();

        return sessionRepository.save(session);
    }

    /**
     * Find active session by session token
     */
    public Optional<Session> findActiveSessionByToken(String sessionToken) {
        return sessionRepository.findBySessionToken(sessionToken)
                .filter(session -> session.getIsActive() &&
                        session.getExpiresAt().isAfter(Instant.now()));
    }

    /**
     * Find active session by refresh token hash
     */
    public Optional<Session> findActiveSessionByRefreshToken(String refreshTokenHash) {
        return sessionRepository.findByRefreshTokenHash(refreshTokenHash)
                .filter(session -> session.getIsActive() &&
                        session.getExpiresAt().isAfter(Instant.now()));
    }

    /**
     * Get all active sessions for a seller
     */
    public List<Session> getActiveSessions(UUID sellerId) {
        return sessionRepository.findBySellerIdAndIsActive(sellerId, true);
    }

    /**
     * Update session last accessed time
     */
    @Transactional
    public void updateLastAccessed(UUID sessionId) {
        sessionRepository.findById(sessionId)
                .ifPresent(session -> {
                    session.setLastAccessedAt(Instant.now());
                    sessionRepository.save(session);
                });
    }

    /**
     * Invalidate a specific session
     */
    @Transactional
    public void invalidateSession(UUID sessionId) {
        sessionRepository.findById(sessionId)
                .ifPresent(session -> {
                    session.setIsActive(false);
                    session.setRevokedAt(Instant.now());
                    sessionRepository.save(session);
                    log.info("Session invalidated: {}", sessionId);
                });
    }

    /**
     * Invalidate all sessions for a seller
     */
    @Transactional
    public void invalidateAllSessions(UUID sellerId) {
        List<Session> activeSessions = sessionRepository.findBySellerIdAndIsActive(sellerId, true);

        activeSessions.forEach(session -> {
            session.setIsActive(false);
            session.setRevokedAt(Instant.now());
        });

        if (!activeSessions.isEmpty()) {
            sessionRepository.saveAll(activeSessions);
            log.info("Invalidated {} sessions for seller: {}", activeSessions.size(), sellerId);
        }
    }

    /**
     * Invalidate session by device ID
     */
    @Transactional
    public void invalidateSessionByDevice(UUID sellerId, String deviceId) {
        sessionRepository.findBySellerIdAndIsActive(sellerId, true).stream()
                .filter(session -> deviceId.equals(session.getDeviceId()))
                .findFirst()
                .ifPresent(session -> {
                    session.setIsActive(false);
                    session.setRevokedAt(Instant.now());
                    sessionRepository.save(session);
                    log.info("Session invalidated for seller: {}, device: {}", sellerId, deviceId);
                });
    }

    /**
     * Mark session as suspicious
     */
    @Transactional
    public void markAsSuspicious(UUID sessionId, String reason) {
        sessionRepository.findById(sessionId)
                .ifPresent(session -> {
                    session.setIsSuspicious(true);
                    session.setIsActive(false);
                    session.setRevokedAt(Instant.now());
                    sessionRepository.save(session);
                    log.warn("Session marked as suspicious: {}, Reason: {}", sessionId, reason);
                });
    }

    /**
     * Clean up expired sessions
     */
    @Transactional
    public void cleanupExpiredSessions() {
        Instant cutoff = Instant.now();
        int deleted = sessionRepository.deleteExpiredSessions(cutoff);

        if (deleted > 0) {
            log.info("Cleaned up {} expired sessions", deleted);
        }
    }

    /**
     * Check if session is valid
     */
    public boolean isValidSession(UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .map(session -> session.getIsActive() &&
                        session.getExpiresAt().isAfter(Instant.now()))
                .orElse(false);
    }

    /**
     * Get session count for a seller
     */
    public int getActiveSessionCount(UUID sellerId) {
        return sessionRepository.findBySellerIdAndIsActive(sellerId, true).size();
    }

    /**
     * Refresh session expiration
     */
    @Transactional
    public void refreshSessionExpiration(UUID sessionId, int additionalDays) {
        sessionRepository.findById(sessionId)
                .ifPresent(session -> {
                    session.setExpiresAt(Instant.now().plusSeconds(additionalDays * 24 * 3600L));
                    sessionRepository.save(session);
                });
    }
}