package com.stack.sellstack.security;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@UtilityClass
public class SecurityUtils {

    private static final Argon2 ARGON2 = Argon2Factory.create(
            Argon2Factory.Argon2Types.ARGON2id,
            16, // Salt length
            32  // Hash length
    );

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static String generateDeviceFingerprint(HttpServletRequest request) {
        StringBuilder fingerprint = new StringBuilder();

        fingerprint.append(request.getHeader("User-Agent"))
                .append(request.getHeader("Accept-Language"))
                .append(request.getHeader("Accept-Encoding"));

        String screenResolution = request.getHeader("X-Screen-Resolution");
        if (StringUtils.isNotBlank(screenResolution)) {
            fingerprint.append(screenResolution);
        }

        return hashString(fingerprint.toString());
    }

    public static String getClientIP(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    public static String hashOtp(String otp) {
        return ARGON2.hash(2, 65536, 1, otp.getBytes(StandardCharsets.UTF_8));
    }

    public static boolean verifyOtp(String otp, String hashedOtp) {
        return ARGON2.verify(hashedOtp, otp.getBytes(StandardCharsets.UTF_8));
    }

    public static String generateSecureRandomString(int length) {
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}