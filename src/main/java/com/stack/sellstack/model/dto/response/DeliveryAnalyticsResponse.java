package com.stack.sellstack.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAnalyticsResponse {

    private long totalOrders;
    private long emailDeliveries;
    private long whatsappDeliveries;
    private double avgDeliveryTimeHours;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
}