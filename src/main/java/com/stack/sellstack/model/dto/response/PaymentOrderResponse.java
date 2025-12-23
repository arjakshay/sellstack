package com.stack.sellstack.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderResponse {
    private String orderId;
    private Integer amount;
    private String currency;
    private String receipt;
    private String status;
    private Instant createdAt;
    private String razorpayKeyId;
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> notes;
}
