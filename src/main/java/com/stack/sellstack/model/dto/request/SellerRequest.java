package com.stack.sellstack.model.dto.request;

import com.stack.sellstack.model.enums.SellerStatus;
import com.stack.sellstack.model.enums.VerificationStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SellerRequest {

    @Data
    public static class UpdateProfileRequest {
        @Size(min = 3, max = 100, message = "Display name must be 3-100 characters")
        private String displayName;

        @Size(max = 255, message = "Full name must be less than 255 characters")
        private String fullName;

        @Size(max = 2000, message = "Bio must be less than 2000 characters")
        private String bio;

        @Pattern(regexp = "^(http|https)://.*$", message = "Invalid URL format")
        private String profilePicUrl;

        @Size(max = 100, message = "UPI ID must be less than 100 characters")
        private String upiId;

        @Size(min = 15, max = 15, message = "GST number must be 15 characters")
        private String gstNumber;

        @Size(min = 10, max = 10, message = "PAN number must be 10 characters")
        private String panNumber;

        private Boolean marketingConsent;
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank(message = "Current password is required")
        private String currentPassword;

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 100, message = "Password must be 8-100 characters")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
                message = "Password must contain uppercase, lowercase, number and special character")
        private String newPassword;

        @NotBlank(message = "Confirm password is required")
        private String confirmPassword;
    }

    @Data
    public static class PayoutRequest {
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "100.00", message = "Minimum payout amount is ₹100")
        @DecimalMax(value = "1000000.00", message = "Maximum payout amount is ₹10,00,000")
        private BigDecimal amount;

        @NotBlank(message = "Payout method is required")
        private String payoutMethod; // UPI, BANK, PAYPAL

        private String payoutDetails; // UPI ID or bank details JSON
    }

    @Data
    public static class UpdateStatusRequest {
        @NotNull(message = "Status is required")
        private SellerStatus status;

        private String reason;
    }

    @Data
    public static class AdjustBalanceRequest {
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be positive")
        private BigDecimal amount;

        @NotBlank(message = "Type is required")
        private String type; // CREDIT, DEBIT

        @NotBlank(message = "Reason is required")
        private String reason;

        private String referenceId;

        private String notes;
    }

    @Data
    public static class SearchRequest {
        private String query;
        private SellerStatus status;
        private VerificationStatus verificationStatus;
        private LocalDate createdFrom;
        private LocalDate createdTo;
        private BigDecimal minBalance;
        private BigDecimal maxBalance;
    }
}