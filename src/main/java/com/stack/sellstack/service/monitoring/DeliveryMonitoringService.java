package com.stack.sellstack.service.monitoring;

import com.stack.sellstack.model.entity.DeliveryAnalytics;
import com.stack.sellstack.repository.DeliveryAnalyticsRepository;
import com.stack.sellstack.repository.EmailQueueRepository;
import com.stack.sellstack.repository.WhatsAppQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeliveryMonitoringService {

    private final EmailQueueRepository emailQueueRepository;
    private final WhatsAppQueueRepository whatsAppQueueRepository;
    private final DeliveryAnalyticsRepository analyticsRepository;
    private final AlertService alertService;

    /**
     * Daily aggregation job
     */
    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM daily
    @Transactional
    public void aggregateDailyAnalytics() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        log.info("Aggregating delivery analytics for {}", yesterday);

        try {
            // Aggregate email statistics
            Map<String, Object> emailStats = emailQueueRepository.getDailyStatistics(yesterday);

            // Aggregate WhatsApp statistics
            Map<String, Object> whatsappStats = whatsAppQueueRepository.getDailyStatistics(yesterday);

            // Save aggregated data
            saveAggregatedData(yesterday, emailStats, whatsappStats);

            // Check for alerts
            checkForAlerts(emailStats, whatsappStats);

        } catch (Exception e) {
            log.error("Failed to aggregate daily analytics for {}", yesterday, e);
            alertService.sendAlert("ANALYTICS_AGGREGATION_FAILED",
                    "Failed to aggregate daily analytics: " + e.getMessage());
        }
    }

    /**
     * Check delivery failures
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void checkDeliveryFailures() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);

        try {
            // Check for stuck emails
            long stuckEmails = emailQueueRepository.countStuckEmails(threshold);
            if (stuckEmails > 10) {
                alertService.sendAlert("EMAIL_QUEUE_STUCK",
                        String.format("%d emails stuck in queue for more than 30 minutes", stuckEmails));
            }

            // Check for failed WhatsApp messages
            long failedWhatsApps = whatsAppQueueRepository.countRecentFailures(threshold);
            if (failedWhatsApps > 5) {
                alertService.sendAlert("WHATSAPP_DELIVERY_ISSUE",
                        String.format("%d WhatsApp messages failed in last 30 minutes", failedWhatsApps));
            }

        } catch (Exception e) {
            log.error("Failed to check delivery failures", e);
        }
    }

    private void saveAggregatedData(LocalDate date,
                                    Map<String, Object> emailStats,
                                    Map<String, Object> whatsappStats) {

        // Save email analytics
        DeliveryAnalytics emailAnalytics = DeliveryAnalytics.builder()
                .date(date)
                .channel("EMAIL")
                .attempted(getLongValue(emailStats, "attempted"))
                .sent(getLongValue(emailStats, "sent"))
                .delivered(getLongValue(emailStats, "delivered"))
                .failed(getLongValue(emailStats, "failed"))
                .bounced(getLongValue(emailStats, "bounced"))
                .opened(getLongValue(emailStats, "opened"))
                .clicked(getLongValue(emailStats, "clicked"))
                .build();

        analyticsRepository.save(emailAnalytics);

        // Save WhatsApp analytics
        DeliveryAnalytics whatsappAnalytics = DeliveryAnalytics.builder()
                .date(date)
                .channel("WHATSAPP")
                .attempted(getLongValue(whatsappStats, "attempted"))
                .sent(getLongValue(whatsappStats, "sent"))
                .delivered(getLongValue(whatsappStats, "delivered"))
                .failed(getLongValue(whatsappStats, "failed"))
                .build();

        analyticsRepository.save(whatsappAnalytics);

        log.info("Daily analytics saved for {}", date);
    }

    private Long getLongValue(Map<String, Object> stats, String key) {
        if (stats == null || !stats.containsKey(key)) {
            return 0L;
        }

        Object value = stats.get(key);
        if (value == null) {
            return 0L;
        }

        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof BigDecimal) {
            return ((BigDecimal) value).longValue();
        }

        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Could not parse {} as Long: {}", key, value);
            return 0L;
        }
    }

    private void checkForAlerts(Map<String, Object> emailStats,
                                Map<String, Object> whatsappStats) {

        long emailAttempted = getLongValue(emailStats, "attempted");
        long emailFailed = getLongValue(emailStats, "failed");
        long emailBounced = getLongValue(emailStats, "bounced");

        long whatsappAttempted = getLongValue(whatsappStats, "attempted");
        long whatsappFailed = getLongValue(whatsappStats, "failed");

        // Check email failure rate
        if (emailAttempted > 0) {
            double failureRate = (double) emailFailed / emailAttempted * 100;
            if (failureRate > 10) { // More than 10% failure rate
                alertService.sendAlert("EMAIL_FAILURE_RATE_HIGH",
                        String.format("Email failure rate: %.2f%% (Failed: %d, Attempted: %d)",
                                failureRate, emailFailed, emailAttempted));
            }

            double bounceRate = (double) emailBounced / emailAttempted * 100;
            if (bounceRate > 5) { // More than 5% bounce rate
                alertService.sendAlert("EMAIL_BOUNCE_RATE_HIGH",
                        String.format("Email bounce rate: %.2f%% (Bounced: %d, Attempted: %d)",
                                bounceRate, emailBounced, emailAttempted));
            }
        }

        // Check WhatsApp failure rate
        if (whatsappAttempted > 0) {
            double failureRate = (double) whatsappFailed / whatsappAttempted * 100;
            if (failureRate > 15) { // More than 15% failure rate
                alertService.sendAlert("WHATSAPP_FAILURE_RATE_HIGH",
                        String.format("WhatsApp failure rate: %.2f%% (Failed: %d, Attempted: %d)",
                                failureRate, whatsappFailed, whatsappAttempted));
            }
        }
    }

    /**
     * Get delivery analytics for a specific date range
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDeliveryAnalytics(LocalDate startDate, LocalDate endDate, String channel) {
        Map<String, Object> analytics = new HashMap<>();

        List<DeliveryAnalytics> dailyStats = analyticsRepository.findByDateBetweenAndChannel(
                startDate, endDate, channel);

        long totalAttempted = 0;
        long totalSent = 0;
        long totalDelivered = 0;
        long totalFailed = 0;
        long totalBounced = 0;
        long totalOpened = 0;
        long totalClicked = 0;

        for (DeliveryAnalytics stat : dailyStats) {
            totalAttempted += stat.getAttempted() != null ? stat.getAttempted() : 0;
            totalSent += stat.getSent() != null ? stat.getSent() : 0;
            totalDelivered += stat.getDelivered() != null ? stat.getDelivered() : 0;
            totalFailed += stat.getFailed() != null ? stat.getFailed() : 0;
            totalBounced += stat.getBounced() != null ? stat.getBounced() : 0;
            totalOpened += stat.getOpened() != null ? stat.getOpened() : 0;
            totalClicked += stat.getClicked() != null ? stat.getClicked() : 0;
        }

        analytics.put("period", startDate + " to " + endDate);
        analytics.put("channel", channel);
        analytics.put("attempted", totalAttempted);
        analytics.put("sent", totalSent);
        analytics.put("delivered", totalDelivered);
        analytics.put("failed", totalFailed);
        analytics.put("bounced", totalBounced);
        analytics.put("opened", totalOpened);
        analytics.put("clicked", totalClicked);

        if (totalAttempted > 0) {
            analytics.put("successRate", (double) totalSent / totalAttempted * 100);
            analytics.put("deliveryRate", (double) totalDelivered / totalAttempted * 100);
            analytics.put("failureRate", (double) totalFailed / totalAttempted * 100);
            analytics.put("bounceRate", (double) totalBounced / totalAttempted * 100);
            analytics.put("openRate", totalSent > 0 ? (double) totalOpened / totalSent * 100 : 0);
            analytics.put("clickRate", totalOpened > 0 ? (double) totalClicked / totalOpened * 100 : 0);
        }

        return analytics;
    }
}