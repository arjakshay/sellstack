package com.stack.sellstack.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDetails {
    private String paymentId;
    private String orderId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String paymentMethod;
    private String description;
    private String email;
    private String contact;
    private BigDecimal fee;
    private BigDecimal tax;
    private Instant createdAt;
    private Map<String, Object> notes;
    private String upiId;
    private String upiApp;
    private String qrCodeUrl;
}