package com.stack.sellstack.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaymentsResponse {
    private String orderId;
    private BigDecimal totalAmount;
    private Integer totalPayments;
    private List<PaymentDetails> payments;
    private Summary summary;

    @Data
    @Builder
    public static class Summary {
        private BigDecimal successfulAmount;
        private BigDecimal pendingAmount;
        private BigDecimal failedAmount;
        private BigDecimal refundedAmount;
    }
}
