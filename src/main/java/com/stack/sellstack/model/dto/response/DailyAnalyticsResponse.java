package com.stack.sellstack.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyAnalyticsResponse {
    private LocalDate date;
    private String channel;

    // Sales analytics fields
    private Integer salesCount;
    private BigDecimal revenue;
    private Integer newCustomers;
    private Integer productViews;
    private BigDecimal conversionRate;

    // Communication analytics
    private EmailStats emailStats;
    private WhatsAppStats whatsappStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailStats {
        private long total;
        private long sent;
        private long delivered;
        private long failed;
        private long opened;
        private long clicked;
        private double deliveryRate;
        private double openRate;
        private double clickRate;
        private double bounceRate;
        private double complaintRate;

        @Builder.Default
        private double avgDeliveryTime = 0.0; // in seconds
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WhatsAppStats {
        private long total;
        private long sent;
        private long delivered;
        private long failed;
        private long read;
        private double deliveryRate;
        private double readRate;

        @Builder.Default
        private double avgDeliveryTime = 0.0; // in seconds
        @Builder.Default
        private double avgReadTime = 0.0; // in seconds
    }
}