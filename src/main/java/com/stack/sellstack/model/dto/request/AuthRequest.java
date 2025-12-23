package com.stack.sellstack.model.dto.request;


import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

public class AuthRequest {

    @Data
    public static class InitiateRegistrationRequest {
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian phone number")
        private String phone;

        @Email(message = "Invalid email format")
        private String email;

        @NotNull(message = "Terms must be accepted")
        @AssertTrue(message = "You must accept terms and conditions")
        private Boolean acceptTerms;

        private Boolean marketingConsent;
    }

    @Data
    public static class VerifyRegistrationRequest {
        @NotBlank(message = "Phone number is required")
        private String phone;

        @NotBlank(message = "OTP is required")
        @Size(min = 6, max = 6, message = "OTP must be 6 digits")
        private String otpCode;

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be 8-100 characters")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
                message = "Password must contain at least one uppercase, lowercase, number and special character")
        private String password;

        @NotBlank(message = "Confirm password is required")
        private String confirmPassword;

        @Size(max = 100, message = "Display name must be less than 100 characters")
        private String displayName;

        @Email(message = "Invalid email format")
        private String email;
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "Username is required")
        private String username; // Can be phone or email

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    public static class ForgotPasswordRequest {
        @NotBlank(message = "Phone number is required")
        private String phone;
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank(message = "Phone number is required")
        private String phone;

        @NotBlank(message = "OTP is required")
        @Size(min = 6, max = 6, message = "OTP must be 6 digits")
        private String otpCode;

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 100, message = "Password must be 8-100 characters")
        private String newPassword;

        @NotBlank(message = "Confirm password is required")
        private String confirmPassword;
    }

    @Data
    public static class RefreshTokenRequest {
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;
    }

    @Data
    public static class ResendOTPRequest {
        @NotBlank(message = "Phone number is required")
        private String phone;

        @NotBlank(message = "OTP type is required")
        private String otpType;
    }
}