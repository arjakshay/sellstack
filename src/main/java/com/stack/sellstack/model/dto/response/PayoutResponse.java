package com.stack.sellstack.model.dto.response;

import com.stack.sellstack.model.entity.Payout;
import com.stack.sellstack.model.enums.PayoutStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutResponse {
    private UUID id;
    private UUID sellerId;
    private BigDecimal amount;
    private PayoutStatus status;
    private String payoutMethod;
    private String transactionId;
    private String failureReason;
    private Instant requestedAt;
    private Instant processedAt;

    public static PayoutResponse fromEntity(Payout payout) {
        if (payout == null) return null;

        return PayoutResponse.builder()
                .id(payout.getId())
                .sellerId(payout.getSellerId())
                .amount(payout.getAmount())
                .status(payout.getStatus())
                .payoutMethod(payout.getPayoutMethod())
                .transactionId(payout.getTransactionId())
                .failureReason(payout.getFailureReason())
                .requestedAt(payout.getRequestedAt())
                .processedAt(payout.getProcessedAt())
                .build();
    }
}