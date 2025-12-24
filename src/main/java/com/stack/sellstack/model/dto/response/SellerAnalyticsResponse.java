package com.stack.sellstack.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;  // Change from Instant
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerAnalyticsResponse {
    private UUID sellerId;
    private BigDecimal totalRevenue;
    private Integer totalSales;
    private Integer totalProducts;
    private Integer activeProducts;
    private BigDecimal conversionRate;
    private BigDecimal averageOrderValue;
    private Integer uniqueCustomers;
    private BigDecimal monthlyRecurringRevenue;
    private LocalDateTime lastSaleAt;  // Changed from Instant to LocalDateTime
    private DailyAnalyticsResponse today;
    private DailyAnalyticsResponse yesterday;
    private DailyAnalyticsResponse thisWeek;
    private DailyAnalyticsResponse thisMonth;
}