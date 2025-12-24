package com.stack.sellstack.model.dto.response;


import com.stack.sellstack.model.entity.BalanceTransaction;
import com.stack.sellstack.model.enums.TransactionStatus;
import com.stack.sellstack.model.enums.TransactionType;
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
public class BalanceTransactionResponse {
    private UUID id;
    private UUID sellerId;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionStatus status;
    private String reference;
    private String description;
    private Instant createdAt;

    public static BalanceTransactionResponse fromEntity(BalanceTransaction transaction) {
        if (transaction == null) return null;

        return BalanceTransactionResponse.builder()
                .id(transaction.getId())
                .sellerId(transaction.getSellerId())
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .reference(transaction.getReference())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
