package com.stack.sellstack.model.dto.response;

import com.stack.sellstack.model.entity.SellerBalance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerBalanceResponse {
    private UUID sellerId;
    private BigDecimal availableBalance;
    private BigDecimal pendingBalance;
    private BigDecimal totalEarnings;
    private Instant lastPayoutAt;
    private LocalDate nextPayoutDate;
    private Instant updatedAt;

    public static SellerBalanceResponse fromEntity(SellerBalance balance) {
        if (balance == null) return null;

        return SellerBalanceResponse.builder()
                .sellerId(balance.getSellerId())
                .availableBalance(balance.getAvailableBalance())
                .pendingBalance(balance.getPendingBalance())
                .totalEarnings(balance.getTotalEarnings())
                .lastPayoutAt(balance.getLastPayoutAt())
                .nextPayoutDate(balance.getNextPayoutDate())
                .updatedAt(balance.getUpdatedAt())
                .build();
    }
}
