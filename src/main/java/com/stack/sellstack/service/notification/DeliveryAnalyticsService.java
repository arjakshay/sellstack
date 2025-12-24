package com.stack.sellstack.service.notification;

import com.stack.sellstack.model.dto.response.DailyAnalyticsResponse;
import com.stack.sellstack.repository.EmailQueueRepository;
import com.stack.sellstack.repository.WhatsAppQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DeliveryAnalyticsService {

    private final EmailQueueRepository emailQueueRepository;
    private final WhatsAppQueueRepository whatsAppQueueRepository;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public DailyAnalyticsResponse getDailyAnalytics(String date, String channel, String sellerId) {
        LocalDate analyticsDate = LocalDate.parse(date, dateFormatter);
        LocalDate startOfDay = analyticsDate.atStartOfDay().toLocalDate();
        LocalDate endOfDay = analyticsDate.plusDays(1).atStartOfDay().toLocalDate();

        DailyAnalyticsResponse.DailyAnalyticsResponseBuilder builder = DailyAnalyticsResponse.builder()
                .date(analyticsDate)
                .channel(channel);

        if (channel == null || channel.equals("EMAIL") || channel.equals("ALL")) {
            // UPDATED METHOD NAMES
            long totalEmails = sellerId != null ?
                    emailQueueRepository.countEmailsBySellerIdAndCreatedAtBetween(sellerId, startOfDay, endOfDay) :
                    emailQueueRepository.countEmailsByCreatedAtBetween(startOfDay, endOfDay);

            long sentEmails = sellerId != null ?
                    emailQueueRepository.countEmailsBySellerIdAndStatusAndCreatedAtBetween(sellerId, "SENT", startOfDay, endOfDay) :
                    emailQueueRepository.countEmailsByStatusAndCreatedAtBetween("SENT", startOfDay, endOfDay);

            long deliveredEmails = sellerId != null ?
                    emailQueueRepository.countEmailsBySellerIdAndStatusAndCreatedAtBetween(sellerId, "DELIVERED", startOfDay, endOfDay) :
                    emailQueueRepository.countEmailsByStatusAndCreatedAtBetween("DELIVERED", startOfDay, endOfDay);

            long failedEmails = sellerId != null ?
                    emailQueueRepository.countEmailsBySellerIdAndStatusAndCreatedAtBetween(sellerId, "FAILED", startOfDay, endOfDay) :
                    emailQueueRepository.countEmailsByStatusAndCreatedAtBetween("FAILED", startOfDay, endOfDay);

            long openedEmails = sellerId != null ?
                    emailQueueRepository.countEmailsBySellerIdAndOpenedAtBetween(sellerId, startOfDay, endOfDay) :
                    emailQueueRepository.countEmailsByOpenedAtBetween(startOfDay, endOfDay);

            long clickedEmails = sellerId != null ?
                    emailQueueRepository.countEmailsBySellerIdAndClickedAtBetween(sellerId, startOfDay, endOfDay) :
                    emailQueueRepository.countEmailsByClickedAtBetween(startOfDay, endOfDay);

            builder.emailStats(DailyAnalyticsResponse.EmailStats.builder()
                    .total(totalEmails)
                    .sent(sentEmails)
                    .delivered(deliveredEmails)
                    .failed(failedEmails)
                    .opened(openedEmails)
                    .clicked(clickedEmails)
                    .deliveryRate(totalEmails > 0 ? (deliveredEmails * 100.0 / totalEmails) : 0.0)
                    .openRate(deliveredEmails > 0 ? (openedEmails * 100.0 / deliveredEmails) : 0.0)
                    .clickRate(openedEmails > 0 ? (clickedEmails * 100.0 / openedEmails) : 0.0)
                    .build());
        }

        if (channel == null || channel.equals("WHATSAPP") || channel.equals("ALL")) {
            // WhatsApp methods might also need updating if you changed them
            long totalWhatsApp = sellerId != null ?
                    whatsAppQueueRepository.countBySellerIdAndCreatedAtBetween(sellerId, startOfDay, endOfDay) :
                    whatsAppQueueRepository.countByCreatedAtBetween(startOfDay, endOfDay);

            long sentWhatsApp = sellerId != null ?
                    whatsAppQueueRepository.countBySellerIdAndStatusAndCreatedAtBetween(sellerId, "SENT", startOfDay, endOfDay) :
                    whatsAppQueueRepository.countByStatusAndCreatedAtBetween("SENT", startOfDay, endOfDay);

            long deliveredWhatsApp = sellerId != null ?
                    whatsAppQueueRepository.countBySellerIdAndStatusAndCreatedAtBetween(sellerId, "DELIVERED", startOfDay, endOfDay) :
                    whatsAppQueueRepository.countByStatusAndCreatedAtBetween("DELIVERED", startOfDay, endOfDay);

            long failedWhatsApp = sellerId != null ?
                    whatsAppQueueRepository.countBySellerIdAndStatusAndCreatedAtBetween(sellerId, "FAILED", startOfDay, endOfDay) :
                    whatsAppQueueRepository.countByStatusAndCreatedAtBetween("FAILED", startOfDay, endOfDay);

            long readWhatsApp = sellerId != null ?
                    whatsAppQueueRepository.countBySellerIdAndReadAtBetween(sellerId, startOfDay, endOfDay) :
                    whatsAppQueueRepository.countByReadAtBetween(startOfDay, endOfDay);

            builder.whatsappStats(DailyAnalyticsResponse.WhatsAppStats.builder()
                    .total(totalWhatsApp)
                    .sent(sentWhatsApp)
                    .delivered(deliveredWhatsApp)
                    .failed(failedWhatsApp)
                    .read(readWhatsApp)
                    .deliveryRate(totalWhatsApp > 0 ? (deliveredWhatsApp * 100.0 / totalWhatsApp) : 0.0)
                    .readRate(deliveredWhatsApp > 0 ? (readWhatsApp * 100.0 / deliveredWhatsApp) : 0.0)
                    .build());
        }

        return builder.build();
    }

    public DailyAnalyticsResponse getAnalyticsSummary(LocalDate startDate, LocalDate endDate, String sellerId) {
        DailyAnalyticsResponse.DailyAnalyticsResponseBuilder builder = DailyAnalyticsResponse.builder()
                .date(startDate)
                .channel("ALL");

        // Email stats for the period - UPDATED METHOD NAMES
        long totalEmails = sellerId != null ?
                emailQueueRepository.countEmailsBySellerIdAndCreatedAtBetween(sellerId, startDate, endDate) :
                emailQueueRepository.countEmailsByCreatedAtBetween(startDate, endDate);

        long deliveredEmails = sellerId != null ?
                emailQueueRepository.countEmailsBySellerIdAndStatusAndCreatedAtBetween(sellerId, "DELIVERED", startDate, endDate) :
                emailQueueRepository.countEmailsByStatusAndCreatedAtBetween("DELIVERED", startDate, endDate);

        long openedEmails = sellerId != null ?
                emailQueueRepository.countEmailsBySellerIdAndOpenedAtBetween(sellerId, startDate, endDate) :
                emailQueueRepository.countEmailsByOpenedAtBetween(startDate, endDate);

        long clickedEmails = sellerId != null ?
                emailQueueRepository.countEmailsBySellerIdAndClickedAtBetween(sellerId, startDate, endDate) :
                emailQueueRepository.countEmailsByClickedAtBetween(startDate, endDate);

        // WhatsApp stats for the period
        long totalWhatsApp = sellerId != null ?
                whatsAppQueueRepository.countBySellerIdAndCreatedAtBetween(sellerId, startDate, endDate) :
                whatsAppQueueRepository.countByCreatedAtBetween(startDate, endDate);

        long deliveredWhatsApp = sellerId != null ?
                whatsAppQueueRepository.countBySellerIdAndStatusAndCreatedAtBetween(sellerId, "DELIVERED", startDate, endDate) :
                whatsAppQueueRepository.countByStatusAndCreatedAtBetween("DELIVERED", startDate, endDate);

        long readWhatsApp = sellerId != null ?
                whatsAppQueueRepository.countBySellerIdAndReadAtBetween(sellerId, startDate, endDate) :
                whatsAppQueueRepository.countByReadAtBetween(startDate, endDate);

        builder.emailStats(DailyAnalyticsResponse.EmailStats.builder()
                        .total(totalEmails)
                        .delivered(deliveredEmails)
                        .opened(openedEmails)
                        .clicked(clickedEmails)
                        .deliveryRate(totalEmails > 0 ? (deliveredEmails * 100.0 / totalEmails) : 0.0)
                        .openRate(deliveredEmails > 0 ? (openedEmails * 100.0 / deliveredEmails) : 0.0)
                        .clickRate(openedEmails > 0 ? (clickedEmails * 100.0 / openedEmails) : 0.0)
                        .build())
                .whatsappStats(DailyAnalyticsResponse.WhatsAppStats.builder()
                        .total(totalWhatsApp)
                        .delivered(deliveredWhatsApp)
                        .read(readWhatsApp)
                        .deliveryRate(totalWhatsApp > 0 ? (deliveredWhatsApp * 100.0 / totalWhatsApp) : 0.0)
                        .readRate(deliveredWhatsApp > 0 ? (readWhatsApp * 100.0 / deliveredWhatsApp) : 0.0)
                        .build());

        return builder.build();
    }

    public double getAverageDeliveryTime(String channel, String sellerId) {
        LocalDate last30Days = LocalDate.now().minus(30, ChronoUnit.DAYS);

        if (channel.equals("EMAIL") || channel.equals("ALL")) {
            return sellerId != null ?
                    emailQueueRepository.averageDeliveryTimeBySellerIdAndSentAtAfter(sellerId, last30Days) :
                    emailQueueRepository.averageDeliveryTimeBySentAtAfter(last30Days);
        } else {
            return sellerId != null ?
                    whatsAppQueueRepository.averageDeliveryTimeBySellerIdAndSentAtAfter(sellerId, last30Days) :
                    whatsAppQueueRepository.averageDeliveryTimeBySentAtAfter(last30Days);
        }
    }

}