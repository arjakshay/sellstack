package com.stack.sellstack.service;

import com.stack.sellstack.model.dto.response.SellerAnalyticsResponse;
import com.stack.sellstack.model.dto.response.DailyAnalyticsResponse;
import com.stack.sellstack.model.entity.Seller;
import com.stack.sellstack.repository.SellerAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SellerAnalyticsService {

    private final SellerAnalyticsRepository sellerAnalyticsRepository;
    private final SellerService sellerService;

    public SellerAnalyticsResponse getSellerAnalytics(UUID sellerId, LocalDate startDate, LocalDate endDate) {
        Seller seller = sellerService.findById(sellerId);

        // Set default date range if not provided (last 30 days)
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        // Get analytics data
        BigDecimal totalRevenue = sellerAnalyticsRepository.getTotalRevenue(sellerId, startDateTime, endDateTime);
        Integer totalSales = sellerAnalyticsRepository.getTotalSales(sellerId, startDateTime, endDateTime);
        Integer totalProducts = seller.getTotalProducts();
        Integer activeProducts = sellerAnalyticsRepository.getActiveProductsCount(sellerId);
        Integer uniqueCustomers = sellerAnalyticsRepository.getUniqueCustomers(sellerId, startDateTime, endDateTime);

        // Calculate conversion rate
        Integer totalViews = sellerAnalyticsRepository.getTotalProductViews(sellerId, startDateTime, endDateTime);
        BigDecimal conversionRate = totalViews > 0
                ? new BigDecimal(totalSales).divide(new BigDecimal(totalViews), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Calculate average order value
        BigDecimal averageOrderValue = totalSales > 0
                ? totalRevenue.divide(new BigDecimal(totalSales), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Get date-specific analytics
        DailyAnalyticsResponse today = getDailyAnalytics(sellerId, LocalDate.now());
        DailyAnalyticsResponse yesterday = getDailyAnalytics(sellerId, LocalDate.now().minusDays(1));
        DailyAnalyticsResponse thisWeek = getWeeklyAnalytics(sellerId);
        DailyAnalyticsResponse thisMonth = getMonthlyAnalytics(sellerId);

        return SellerAnalyticsResponse.builder()
                .sellerId(sellerId)
                .totalRevenue(totalRevenue)
                .totalSales(totalSales)
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .conversionRate(conversionRate)
                .averageOrderValue(averageOrderValue)
                .uniqueCustomers(uniqueCustomers)
                .monthlyRecurringRevenue(calculateMRR(sellerId))
                .lastSaleAt(sellerAnalyticsRepository.getLastSaleDate(sellerId))
                .today(today)
                .yesterday(yesterday)
                .thisWeek(thisWeek)
                .thisMonth(thisMonth)
                .build();
    }

    public Page<DailyAnalyticsResponse> getDailyAnalytics(UUID sellerId, LocalDate startDate,
                                                          LocalDate endDate, Pageable pageable) {
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        // Get raw data
        Page<Object[]> rawData = sellerAnalyticsRepository.getDailyAnalyticsData(
                sellerId, startDate, endDate, pageable);

        // Convert to DailyAnalyticsResponse
        List<DailyAnalyticsResponse> content = rawData.getContent().stream()
                .map(this::mapToDailyAnalyticsResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, rawData.getTotalElements());
    }

    private DailyAnalyticsResponse mapToDailyAnalyticsResponse(Object[] row) {
        LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
        Long salesCount = (Long) row[1];
        BigDecimal revenue = (BigDecimal) row[2];
        Long newCustomers = (Long) row[3];
        BigDecimal productViews = (BigDecimal) row[4];

        BigDecimal conversionRate = productViews.compareTo(BigDecimal.ZERO) > 0
                ? new BigDecimal(salesCount).divide(productViews, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return DailyAnalyticsResponse.builder()
                .date(date)
                .salesCount(salesCount.intValue())
                .revenue(revenue)
                .newCustomers(newCustomers.intValue())
                .productViews(productViews.intValue())
                .conversionRate(conversionRate)
                .build();
    }

    private DailyAnalyticsResponse getDailyAnalytics(UUID sellerId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        Integer salesCount = sellerAnalyticsRepository.getDailySalesCount(sellerId, startOfDay, endOfDay);
        BigDecimal revenue = sellerAnalyticsRepository.getDailyRevenue(sellerId, startOfDay, endOfDay);
        Integer newCustomers = sellerAnalyticsRepository.getDailyNewCustomers(sellerId, startOfDay, endOfDay);
        Integer productViews = sellerAnalyticsRepository.getDailyProductViews(sellerId, startOfDay);

        BigDecimal conversionRate = productViews > 0
                ? new BigDecimal(salesCount).divide(new BigDecimal(productViews), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return DailyAnalyticsResponse.builder()
                .date(date)
                .salesCount(salesCount)
                .revenue(revenue)
                .newCustomers(newCustomers)
                .productViews(productViews)
                .conversionRate(conversionRate)
                .build();
    }

    private DailyAnalyticsResponse getWeeklyAnalytics(UUID sellerId) {
        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate weekEnd = LocalDate.now().with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));

        LocalDateTime startDateTime = weekStart.atStartOfDay();
        LocalDateTime endDateTime = weekEnd.atTime(23, 59, 59);

        Integer salesCount = sellerAnalyticsRepository.getSalesCount(sellerId, startDateTime, endDateTime);
        BigDecimal revenue = sellerAnalyticsRepository.getRevenue(sellerId, startDateTime, endDateTime);
        Integer newCustomers = sellerAnalyticsRepository.getNewCustomers(sellerId, startDateTime, endDateTime);
        Integer productViews = sellerAnalyticsRepository.getProductViews(sellerId, startDateTime, endDateTime);

        BigDecimal conversionRate = productViews > 0
                ? new BigDecimal(salesCount).divide(new BigDecimal(productViews), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return DailyAnalyticsResponse.builder()
                .date(weekStart)
                .salesCount(salesCount)
                .revenue(revenue)
                .newCustomers(newCustomers)
                .productViews(productViews)
                .conversionRate(conversionRate)
                .build();
    }

    private DailyAnalyticsResponse getMonthlyAnalytics(UUID sellerId) {
        LocalDate monthStart = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
        LocalDate monthEnd = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        LocalDateTime startDateTime = monthStart.atStartOfDay();
        LocalDateTime endDateTime = monthEnd.atTime(23, 59, 59);

        Integer salesCount = sellerAnalyticsRepository.getSalesCount(sellerId, startDateTime, endDateTime);
        BigDecimal revenue = sellerAnalyticsRepository.getRevenue(sellerId, startDateTime, endDateTime);
        Integer newCustomers = sellerAnalyticsRepository.getNewCustomers(sellerId, startDateTime, endDateTime);
        Integer productViews = sellerAnalyticsRepository.getProductViews(sellerId, startDateTime, endDateTime);

        BigDecimal conversionRate = productViews > 0
                ? new BigDecimal(salesCount).divide(new BigDecimal(productViews), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return DailyAnalyticsResponse.builder()
                .date(monthStart)
                .salesCount(salesCount)
                .revenue(revenue)
                .newCustomers(newCustomers)
                .productViews(productViews)
                .conversionRate(conversionRate)
                .build();
    }

    private BigDecimal calculateMRR(UUID sellerId) {
        LocalDate firstDayOfMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
        LocalDate lastDayOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        LocalDateTime startDateTime = firstDayOfMonth.atStartOfDay();
        LocalDateTime endDateTime = lastDayOfMonth.atTime(23, 59, 59);

        BigDecimal monthlyRevenue = sellerAnalyticsRepository.getRevenue(sellerId, startDateTime, endDateTime);

        // For subscription products
        BigDecimal subscriptionMRR = sellerAnalyticsRepository.getSubscriptionMRR(sellerId);

        return monthlyRevenue.add(subscriptionMRR);
    }

    public void recordProductView(UUID productId, UUID sellerId) {
        sellerAnalyticsRepository.recordProductView(productId, sellerId);
    }

    public void recordSale(UUID sellerId, UUID productId, BigDecimal amount, UUID customerId) {
        sellerAnalyticsRepository.recordSale(sellerId, productId, amount, customerId);
    }
}