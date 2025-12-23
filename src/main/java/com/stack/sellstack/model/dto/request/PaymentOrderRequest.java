package com.stack.sellstack.model.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderRequest {
    @NotNull(message = "Amount is required")
    @Min(value = 100, message = "Minimum amount is 1 INR")
    @Max(value = 1000000000, message = "Maximum amount is 1 crore INR")
    private Integer amount;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "INR", message = "Only INR currency is supported")
    private String currency = "INR";

    @NotBlank(message = "Receipt ID is required")
    @Size(max = 40, message = "Receipt ID too long")
    private String receipt;

    @NotBlank(message = "Product ID is required")
    private String productId;

    @NotBlank(message = "Seller ID is required")
    private String sellerId;

    @NotBlank(message = "Buyer ID is required")
    private String buyerId;

    private boolean autoCapture = true;
    private String description;
    private String notes;
}