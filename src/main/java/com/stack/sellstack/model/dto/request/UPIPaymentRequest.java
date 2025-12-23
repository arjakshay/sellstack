package com.stack.sellstack.model.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UPIPaymentRequest {
    @NotNull(message = "Amount is required")
    @Min(value = 100, message = "Minimum amount is 1 INR")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency = "INR";

    private String description;
    private String customerId;
    private String productId;
    private String orderId;

    // For UPI Intent flow
    private boolean intentFlow = false;
    private String upiApp; // "google_pay", "phonepe", "whatsapp", "bhim"

    // For UPI Collect flow
    private String upiId; // user@bank

    @AssertTrue(message = "Either UPI Intent or Collect flow must be specified")
    public boolean isFlowValid() {
        return (intentFlow && upiApp != null) || (!intentFlow && upiId != null);
    }
}