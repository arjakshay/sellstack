package com.stack.sellstack.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDeliveryRequest {

    @NotNull(message = "Order ID is required")
    private UUID orderId;

    @NotBlank(message = "Seller ID is required")
    private String sellerId;

    @NotBlank(message = "Delivery method is required")
    private String deliveryMethod; // EMAIL, WHATSAPP, BOTH
}