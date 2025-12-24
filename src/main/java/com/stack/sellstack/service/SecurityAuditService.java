package com.stack.sellstack.service;

import com.stack.sellstack.exception.BusinessException;
import com.stack.sellstack.model.entity.SecurityAudit;
import com.stack.sellstack.model.enums.SecurityEventType;
import com.stack.sellstack.repository.SecurityAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityAuditService {

    private final SecurityAuditRepository securityAuditRepository;

    @Value("${security.login.max.attempts:5}")
    private int maxLoginAttempts;

    @Value("${security.login.attempt.window.minutes:15}")
    private int loginAttemptWindowMinutes;

    /**
     * Log registration initiated
     */
    @Transactional
    public void logRegistrationInitiated(String phone, String ipAddress) {
        SecurityAudit audit = SecurityAudit.builder()
                .eventType(SecurityEventType.REGISTRATION_INITIATED)
                .username(phone)
                .ipAddress(ipAddress)
                .eventTimestamp(Instant.now())
                .isSuccess(true)
                .details("Registration OTP sent")
                .build();

        securityAuditRepository.save(audit);
        log.debug("Registration initiated: {}, IP: {}", phone, ipAddress);
    }

    /**
     * Log registration completed
     */
    @Transactional
    public void logRegistrationCompleted(UUID sellerId, String ipAddress) {
        SecurityAudit audit = SecurityAudit.builder()
                .eventType(SecurityEventType.REGISTRATION_COMPLETED)
                .sellerId(sellerId)
                .ipAddress(ipAddress)
                .eventTimestamp(Instant.now())
                .isSuccess(true)
                .details("Registration completed successfully")
                .build();

        securityAuditRepository.save(audit);
        log.info("Registration completed: Seller ID: {}, IP: {}", sellerId, ipAddress);
    }

    /**
     * Log login success
     */
    @Transactional
    public void logLoginSuccess(UUID sellerId, String ipAddress) {
        SecurityAudit audit = SecurityAudit.builder()
                .eventType(SecurityEventType.LOGIN_SUCCESS)
                .sellerId(sellerId)
                .ipAddress(ipAddress)
                .eventTimestamp(Instant.now())
                .isSuccess(true)
                .details("Login successful")
                .build();

        securityAuditRepository.save(audit);

        // Clear failed attempts for this IP
        clearFailedAttemptsForIp(ipAddress);

        log.info("Login success: Seller ID: {}, IP: {}", sellerId, ipAddress);
    }

    /**
     * Log login failed
     */
    @Transactional
    public void logLoginFailed(String username, String ipAddress, String reason) {
        // Truncate reason if it's too long
        String truncatedReason = reason;
        if (reason != null && reason.length() > 990) { // Leave room for "Login failed: "
            truncatedReason = reason.substring(0, 990) + "...[truncated]";
        }

        SecurityAudit audit = SecurityAudit.builder()
                .eventType(SecurityEventType.LOGIN_FAILED)
                .username(username)
                .ipAddress(ipAddress)
                .eventTimestamp(Instant.now())
                .isSuccess(false)
                .details("Login failed: " + truncatedReason)
                .build();

        securityAuditRepository.save(audit);
        log.warn("Login failed: Username: {}, IP: {}, Reason: {}", username, ipAddress, reason);
    }

    /**
     * Log logout
     */
    @Transactional
    public void logLogout(String username, String ipAddress) {
        SecurityAudit audit = SecurityAudit.builder()
                .eventType(SecurityEventType.LOGOUT)
                .username(username)
                .ipAddress(ipAddress)
                .eventTimestamp(Instant.now())
                .isSuccess(true)
                .details("User logged out")
                .build();

        securityAuditRepository.save(audit);
        log.info("Logout: Username: {}, IP: {}", username, ipAddress);
    }

    /**
     * Log token refresh
     */
    @Transactional
    public void logTokenRefreshed(String username, String ipAddress) {
        SecurityAudit audit = SecurityAudit.builder()
                .eventType(SecurityEventType.TOKEN_REFRESHED)
                .username(username)
                .ipAddress(ipAddress)
                .eventTimestamp(Instant.now())
                .isSuccess(true)
                .details("Access token refreshed")
                .build();

        securityAuditRepository.save(audit);
        log.debug("Token refreshed: Username: {}, IP: {}", username, ipAddress);
    }

    /**
     * Log password reset initiated
     */
    @Transactional
    public void logPasswordResetInitiated(String phone, String ipAddress) {
        SecurityAudit audit = SecurityAudit.builder()
                .eventType(SecurityEventType.PASSWORD_RESET_INITIATED)
                .username(phone)
                .ipAddress(ipAddress)
                .eventTimestamp(Instant.now())
                .isSuccess(true)
                .details("Password reset OTP sent")
                .build();

        securityAuditRepository.save(audit);
        log.info("Password reset initiated: Phone: {}, IP: {}", phone, ipAddress);
    }

    /**
     * Log password reset completed
     */
    @Transactional
    public void logPasswordResetCompleted(String phone, String ipAddress) {
        SecurityAudit audit = SecurityAudit.builder()
                .eventType(SecurityEventType.PASSWORD_RESET_COMPLETED)
                .username(phone)
                .ipAddress(ipAddress)
                .eventTimestamp(Instant.now())
                .isSuccess(true)
                .details("Password reset completed")
                .build();

        securityAuditRepository.save(audit);
        log.info("Password reset completed: Phone: {}, IP: {}", phone, ipAddress);
    }

    /**
     * Log OTP generated
     */
    @Transactional
    public void logOtpGenerated(String phone, String otpType, String ipAddress) {
        SecurityAudit audit = SecurityAudit.builder()
                .eventType(SecurityEventType.OTP_GENERATED)
                .username(phone)
                .ipAddress(ipAddress)
                .eventTimestamp(Instant.now())
                .isSuccess(true)
                .details("OTP generated for: " + otpType)
                .build();

        securityAuditRepository.save(audit);
    }

    /**
     * Log OTP verification success
     */
    @Transactional
    public void logOtpVerificationSuccess(String phone, String otpType, String ipAddress) {
        SecurityAudit audit = SecurityAudit.builder()
                .eventType(SecurityEventType.OTP_VERIFICATION_SUCCESS)
                .username(phone)
                .ipAddress(ipAddress)
                .eventTimestamp(Instant.now())
                .isSuccess(true)
                .details("OTP verified for: " + otpType)
                .build();

        securityAuditRepository.save(audit);
    }

    /**
     * Log OTP verification failed
     */
    @Transactional
    public void logOtpVerificationFailed(String phone, String otpType, String reason, String ipAddress) {
        SecurityAudit audit = SecurityAudit.builder()
                .eventType(SecurityEventType.OTP_VERIFICATION_FAILED)
                .username(phone)
                .ipAddress(ipAddress)
                .eventTimestamp(Instant.now())
                .isSuccess(false)
                .details("OTP verification failed for " + otpType + ": " + reason)
                .build();

        securityAuditRepository.save(audit);
    }

    /**
     * Log session created
     */
    @Transactional
    public void logSessionCreated(UUID sellerId, String deviceId,
                                  String deviceType, String ipAddress) {
        SecurityAudit audit = SecurityAudit.builder()
                .eventType(SecurityEventType.SESSION_CREATED)
                .sellerId(sellerId)
                .ipAddress(ipAddress)
                .eventTimestamp(Instant.now())
                .isSuccess(true)
                .details(String.format("Session created - Device: %s, Type: %s",
                        deviceId, deviceType))
                .build();

        securityAuditRepository.save(audit);
    }

    /**
     * Log session revoked
     */
    @Transactional
    public void logSessionRevoked(UUID sellerId, String deviceId,
                                  String reason, String ipAddress) {
        SecurityAudit audit = SecurityAudit.builder()
                .eventType(SecurityEventType.SESSION_REVOKED)
                .sellerId(sellerId)
                .ipAddress(ipAddress)
                .eventTimestamp(Instant.now())
                .isSuccess(false)
                .details(String.format("Session revoked - Device: %s, Reason: %s",
                        deviceId, reason))
                .build();

        securityAuditRepository.save(audit);
    }

    /**
     * Check login attempts for rate limiting
     */
    public void checkLoginAttempts(String username, String ipAddress) {
        Instant windowStart = Instant.now().minus(loginAttemptWindowMinutes, ChronoUnit.MINUTES);

        int failedAttempts = securityAuditRepository.countFailedLoginAttempts(
                username, ipAddress, windowStart);

        if (failedAttempts >= maxLoginAttempts) {
            // Log the block attempt
            SecurityAudit audit = SecurityAudit.builder()
                    .eventType(SecurityEventType.LOGIN_BLOCKED)
                    .username(username)
                    .ipAddress(ipAddress)
                    .eventTimestamp(Instant.now())
                    .isSuccess(false)
                    .details("Too many failed attempts: " + failedAttempts)
                    .severity("HIGH")
                    .build();

            securityAuditRepository.save(audit);

            log.warn("Login blocked - Username: {}, IP: {}, Attempts: {}",
                    username, ipAddress, failedAttempts);

            throw new BusinessException("Too many login attempts. Please try again later.");
        }
    }

    /**
     * Clear failed attempts for IP
     */
    @Transactional
    public void clearFailedAttemptsForIp(String ipAddress) {
        // Could update audit records or just log
        log.debug("Cleared failed attempts for IP: {}", ipAddress);
    }

    /**
     * Log suspicious activity
     */
    @Transactional
    public void logSuspiciousActivity(String eventType, String username,
                                      String ipAddress, String details) {
        SecurityAudit audit = SecurityAudit.builder()
                .eventType(SecurityEventType.valueOf(eventType))
                .username(username)
                .ipAddress(ipAddress)
                .eventTimestamp(Instant.now())
                .isSuccess(false)
                .details(details)
                .severity("HIGH")
                .build();

        securityAuditRepository.save(audit);

        log.warn("Suspicious activity: {} - Username: {}, IP: {}, Details: {}",
                eventType, username, ipAddress, details);
    }

    /**
     * Get audit logs for a user
     */
    public List<SecurityAudit> getUserAuditLogs(String username) {
        return securityAuditRepository.findByUsernameOrderByEventTimestampDesc(username);
    }

    /**
     * Get recent security events
     */
    public List<SecurityAudit> getRecentSecurityEvents(int hours, int maxResults) {
        Instant since = Instant.now().minus(hours * 3600, ChronoUnit.SECONDS);

        if (maxResults > 0) {
            // Use Pageable for limited results
            org.springframework.data.domain.Pageable pageable =
                    org.springframework.data.domain.PageRequest.of(0, maxResults);
            return securityAuditRepository.findRecentEvents(since, pageable);
        } else {
            return securityAuditRepository.findRecentEvents(since);
        }
    }
}