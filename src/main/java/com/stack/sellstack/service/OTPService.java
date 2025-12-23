package com.stack.sellstack.service;

import com.stack.sellstack.exception.BusinessException;
import com.stack.sellstack.exception.OTPException;
import com.stack.sellstack.model.entity.OtpStorage;
import com.stack.sellstack.model.enums.OTPType;
import com.stack.sellstack.repository.OtpStorageRepository;
import com.stack.sellstack.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class OTPService {

    private static final String OTP_CHARS = "0123456789";
    private static final int OTP_LENGTH = 6;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final OtpStorageRepository otpStorageRepository;
    private final RateLimitService rateLimitService;
    private final SecurityAuditService securityAuditService;
    private final HttpServletRequest request;

    @Value("${otp.validity.minutes:5}")
    private int otpValidityMinutes;

    @Value("${otp.max.attempts:5}")
    private int maxOtpAttempts;

    @Value("${otp.resend.cooldown.seconds:60}")
    private int resendCooldownSeconds;

    @Transactional
    public void generateAndSendOtp(String phone, String email, OTPType otpType) {
        // Rate limiting
        rateLimitService.checkRateLimit(phone, "OTP_GENERATION", 3, 300);
        rateLimitService.checkRateLimit(
                SecurityUtils.getClientIP(request),
                "IP_OTP_GENERATION",
                10,
                300
        );

        // Check cooldown
        Instant cooldownUntil = Instant.now().minusSeconds(resendCooldownSeconds);
        var recentOtp = otpStorageRepository.findRecentUnverifiedOtp(
                phone, otpType, cooldownUntil);

        if (recentOtp.isPresent()) {
            long remainingSeconds = ChronoUnit.SECONDS.between(
                    Instant.now(), recentOtp.get().getExpiresAt());
            throw new OTPException(
                    String.format("Please wait %d seconds before requesting new OTP",
                            remainingSeconds)
            );
        }

        // Generate OTP
        String otpCode = generateSecureOtp();
        String otpHash = SecurityUtils.hashOtp(otpCode);

        // Store OTP
        OtpStorage otp = OtpStorage.builder()
                .phone(phone)
                .email(email)
                .otpCode(otpCode)
                .otpHash(otpHash)
                .otpType(otpType)
                .deviceFingerprint(SecurityUtils.generateDeviceFingerprint(request))
                .ipAddress(SecurityUtils.getClientIP(request))
                .userAgent(request.getHeader("User-Agent"))
                .expiresAt(Instant.now().plus(otpValidityMinutes, ChronoUnit.MINUTES))
                .attemptCount(0)
                .build();

        otpStorageRepository.save(otp);

        // Send OTP
        sendOtp(phone, email, otpCode, otpType);

        // Audit - Convert OTPType enum to string
        securityAuditService.logOtpGenerated(phone, otpType.toString(), // Convert enum to string
                SecurityUtils.getClientIP(request));

        log.info("OTP generated for {}: {}", phone, otpType);
    }

    @Transactional
    public boolean verifyOtp(String phone, String otpCode, OTPType otpType) {
        var otpOptional = otpStorageRepository.findValidOtp(
                phone, otpType, Instant.now());

        if (otpOptional.isEmpty()) {
            // Convert enum to string
            securityAuditService.logOtpVerificationFailed(phone, otpType.toString(),
                    "NO_VALID_OTP", SecurityUtils.getClientIP(request));
            return false;
        }

        OtpStorage otp = otpOptional.get();

        // Check attempts
        if (otp.getAttemptCount() >= maxOtpAttempts) {
            // Convert enum to string
            securityAuditService.logOtpVerificationFailed(phone, otpType.toString(),
                    "MAX_ATTEMPTS_EXCEEDED", SecurityUtils.getClientIP(request));
            otpStorageRepository.delete(otp);
            throw new OTPException("Maximum OTP attempts exceeded. Please request a new OTP.");
        }

        // Verify hash
        boolean isValid = SecurityUtils.verifyOtp(otpCode, otp.getOtpHash());

        otp.setAttemptCount(otp.getAttemptCount() + 1);
        otp.setLastAttemptAt(Instant.now());

        if (isValid) {
            otp.setVerifiedAt(Instant.now());
            // Convert enum to string
            securityAuditService.logOtpVerificationSuccess(phone, otpType.toString(),
                    SecurityUtils.getClientIP(request));
        } else {
            // Convert enum to string
            securityAuditService.logOtpVerificationFailed(phone, otpType.toString(),
                    "INVALID_CODE", SecurityUtils.getClientIP(request));
        }

        otpStorageRepository.save(otp);
        return isValid;
    }

    private String generateSecureOtp() {
        StringBuilder otp = new StringBuilder(OTP_LENGTH);
        byte[] randomBytes = new byte[OTP_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);

        for (int i = 0; i < OTP_LENGTH; i++) {
            int index = Math.abs(randomBytes[i]) % OTP_CHARS.length();
            otp.append(OTP_CHARS.charAt(index));
        }

        return otp.toString();
    }

    private void sendOtp(String phone, String email, String otpCode, OTPType otpType) {
        try {
            // TODO: Integrate with SMS service (Twilio/Msg91)
            String message = String.format(
                    "Your SellStack %s OTP is %s. Valid for %d minutes.",
                    otpType.name().toLowerCase(),
                    otpCode,
                    otpValidityMinutes
            );

            log.info("OTP sent to {}: {}", phone, otpCode);

        } catch (Exception e) {
            log.error("Failed to send OTP to {}", phone, e);
            throw new BusinessException("Failed to send OTP. Please try again.");
        }
    }

    @Scheduled(fixedRate = 3600000) // Every hour
    @Transactional
    public void cleanupExpiredOtps() {
        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        int deleted = otpStorageRepository.deleteExpiredOtps(cutoff);
        log.info("Cleaned up {} expired OTP records", deleted);
    }
}