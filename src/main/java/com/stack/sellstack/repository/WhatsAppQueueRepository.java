package com.stack.sellstack.repository;

import com.stack.sellstack.model.entity.WhatsAppQueue;
import org.springframework.data.domain.Pageable;
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
public interface WhatsAppQueueRepository extends JpaRepository<WhatsAppQueue, UUID> {

    @Query("SELECT wq FROM WhatsAppQueue wq WHERE wq.status = 'PENDING' AND wq.sendAfter <= :currentTime ORDER BY wq.priority DESC, wq.createdAt ASC")
    List<WhatsAppQueue> findPendingMessages(@Param("currentTime") Date currentTime);

    // FIX: Rename second method or use Pageable
    @Query("SELECT wq FROM WhatsAppQueue wq WHERE wq.status = 'PENDING' AND wq.sendAfter <= :currentTime ORDER BY wq.priority DESC, wq.createdAt ASC")
    List<WhatsAppQueue> findPendingMessagesWithLimit(@Param("currentTime") Date currentTime, Pageable pageable);

    // OR use native query with LIMIT
    @Query(value = "SELECT * FROM whatsapp_queue wq WHERE wq.status = 'PENDING' AND wq.send_after <= :currentTime " +
            "ORDER BY wq.priority DESC, wq.created_at ASC LIMIT :limit",
            nativeQuery = true)
    List<WhatsAppQueue> findPendingMessagesNative(@Param("currentTime") Date currentTime, @Param("limit") int limit);

    Optional<WhatsAppQueue> findByProviderMessageId(String providerMessageId);

    @Query("SELECT COUNT(wq) FROM WhatsAppQueue wq WHERE wq.status = 'PENDING' AND wq.sendAfter <= :threshold")
    long countStuckMessages(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT COUNT(wq) FROM WhatsAppQueue wq WHERE wq.status = 'FAILED' AND wq.updatedAt >= :since")
    long countRecentFailures(@Param("since") LocalDateTime since);

    // FIX: Use FUNCTION('DATE', ...) for LocalDateTime
    @Query("SELECT COUNT(wq) as attempted, " +
            "SUM(CASE WHEN wq.status = 'SENT' THEN 1 ELSE 0 END) as sent, " +
            "SUM(CASE WHEN wq.status = 'DELIVERED' THEN 1 ELSE 0 END) as delivered, " +
            "SUM(CASE WHEN wq.status = 'FAILED' THEN 1 ELSE 0 END) as failed " +
            "FROM WhatsAppQueue wq " +
            "WHERE FUNCTION('DATE', wq.createdAt) = :date")
    Map<String, Object> getDailyStatistics(@Param("date") LocalDate date);

    @Query("SELECT wq FROM WhatsAppQueue wq WHERE wq.sellerId = :sellerId AND wq.status = :status ORDER BY wq.createdAt DESC")
    List<WhatsAppQueue> findBySellerIdAndStatus(@Param("sellerId") String sellerId, @Param("status") String status);

    // FIX: This method has the same DATE() issue
    @Query("SELECT COUNT(wq) FROM WhatsAppQueue wq WHERE wq.sellerId = :sellerId " +
            "AND wq.createdAt >= :startOfDay AND wq.createdAt < :endOfDay")
    long countTodayMessagesBySeller(
            @Param("sellerId") String sellerId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay);

    // TODO: Update method names if you want consistency with EmailQueueRepository
    long countBySellerIdAndCreatedAtBetween(String sellerId, LocalDate start, LocalDate end);
    long countByCreatedAtBetween(LocalDate start, LocalDate end);
    long countBySellerIdAndStatusAndCreatedAtBetween(String sellerId, String status, LocalDate start, LocalDate end);
    long countByStatusAndCreatedAtBetween(String status, LocalDate start, LocalDate end);
    long countBySellerIdAndReadAtBetween(String sellerId, LocalDate start, LocalDate end);
    long countByReadAtBetween(LocalDate start, LocalDate end);

    @Query("SELECT AVG(TIMESTAMPDIFF(SECOND, w.sentAt, w.deliveredAt)) FROM WhatsAppQueue w WHERE w.sentAt IS NOT NULL AND w.deliveredAt IS NOT NULL AND w.sentAt >= :since")
    Double averageDeliveryTimeBySentAtAfter(@Param("since") LocalDate since);

    @Query("SELECT AVG(TIMESTAMPDIFF(SECOND, w.sentAt, w.deliveredAt)) FROM WhatsAppQueue w WHERE w.sellerId = :sellerId AND w.sentAt IS NOT NULL AND w.deliveredAt IS NOT NULL AND w.sentAt >= :since")
    Double averageDeliveryTimeBySellerIdAndSentAtAfter(@Param("sellerId") String sellerId, @Param("since") LocalDate since);
}