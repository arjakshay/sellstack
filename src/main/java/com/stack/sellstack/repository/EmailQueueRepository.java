package com.stack.sellstack.repository;

import com.stack.sellstack.model.entity.EmailQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailQueueRepository extends JpaRepository<EmailQueue, UUID> {

    @Query("SELECT eq FROM EmailQueue eq WHERE eq.status = 'PENDING' AND eq.sendAfter <= :currentTime ORDER BY eq.priority DESC, eq.createdAt ASC")
    List<EmailQueue> findPendingEmails(@Param("currentTime") Date currentTime);

    @Query("SELECT eq FROM EmailQueue eq WHERE eq.status = 'PENDING' AND eq.sendAfter <= :currentTime ORDER BY eq.priority DESC, eq.createdAt ASC")
    List<EmailQueue> findPendingEmails(@Param("currentTime") Date currentTime, @Param("limit") int limit);

    Optional<EmailQueue> findByProviderMessageId(String providerMessageId);

    @Query("SELECT COUNT(eq) FROM EmailQueue eq WHERE eq.status = 'PENDING' AND eq.sendAfter <= :threshold")
    long countStuckEmails(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT COUNT(eq) FROM EmailQueue eq WHERE eq.status = 'FAILED' AND eq.updatedAt >= :since")
    long countRecentFailures(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(eq) as attempted, " +
            "SUM(CASE WHEN eq.status = 'SENT' THEN 1 ELSE 0 END) as sent, " +
            "SUM(CASE WHEN eq.status = 'DELIVERED' THEN 1 ELSE 0 END) as delivered, " +
            "SUM(CASE WHEN eq.status = 'FAILED' THEN 1 ELSE 0 END) as failed, " +
            "SUM(CASE WHEN eq.status = 'BOUNCED' THEN 1 ELSE 0 END) as bounced, " +
            "SUM(eq.openCount) as opened, " +
            "SUM(eq.clickCount) as clicked " +
            "FROM EmailQueue eq " +
            "WHERE DATE(eq.createdAt) = :date")
    Map<String, Object> getDailyStatistics(@Param("date") LocalDate date);

    @Query("SELECT eq FROM EmailQueue eq WHERE eq.sellerId = :sellerId AND eq.status = :status ORDER BY eq.createdAt DESC")
    List<EmailQueue> findBySellerIdAndStatus(@Param("sellerId") String sellerId, @Param("status") String status);

    @Query("SELECT COUNT(eq) FROM EmailQueue eq WHERE eq.sellerId = :sellerId AND DATE(eq.createdAt) = CURRENT_DATE")
    long countTodayEmailsBySeller(@Param("sellerId") String sellerId);

    long countBySellerIdAndCreatedAtBetween(String sellerId, LocalDate start, LocalDate end);
    long countByCreatedAtBetween(LocalDate start, LocalDate end);
    long countBySellerIdAndStatusAndCreatedAtBetween(String sellerId, String status, LocalDate start, LocalDate end);
    long countByStatusAndCreatedAtBetween(String status, LocalDate start, LocalDate end);
    long countBySellerIdAndOpenedAtBetween(String sellerId, LocalDate start, LocalDate end);
    long countByOpenedAtBetween(LocalDate start, LocalDate end);
    long countBySellerIdAndClickedAtBetween(String sellerId, LocalDate start, LocalDate end);
    long countByClickedAtBetween(LocalDate start, LocalDate end);

    @Query("SELECT AVG(TIMESTAMPDIFF(SECOND, e.sentAt, e.deliveredAt)) FROM EmailQueue e WHERE e.sentAt IS NOT NULL AND e.deliveredAt IS NOT NULL AND e.sentAt >= :since")
    Double averageDeliveryTimeBySentAtAfter(@Param("since") LocalDate since);

    @Query("SELECT AVG(TIMESTAMPDIFF(SECOND, e.sentAt, e.deliveredAt)) FROM EmailQueue e WHERE e.sellerId = :sellerId AND e.sentAt IS NOT NULL AND e.deliveredAt IS NOT NULL AND e.sentAt >= :since")
    Double averageDeliveryTimeBySellerIdAndSentAtAfter(@Param("sellerId") String sellerId, @Param("since") LocalDate since);
}