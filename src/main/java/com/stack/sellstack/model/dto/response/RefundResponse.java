package com.stack.sellstack.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponse {
    private String refundId;
    private String paymentId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String speedRequested;
    private String speedProcessed;
    private String receipt;
    private Instant createdAt;
    private Instant processedAt;
}
