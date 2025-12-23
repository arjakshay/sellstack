package com.stack.sellstack.repository;

import com.stack.sellstack.model.entity.DeliveryAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryAnalyticsRepository extends JpaRepository<DeliveryAnalytics, UUID> {

    Optional<DeliveryAnalytics> findByDateAndChannelAndSellerIdAndTemplateKey(
            LocalDate date, String channel, String sellerId, String templateKey);

    List<DeliveryAnalytics> findByDateBetweenAndChannel(
            LocalDate startDate, LocalDate endDate, String channel);

    @Query("SELECT da FROM DeliveryAnalytics da WHERE da.date = :date AND da.channel = :channel ORDER BY da.createdAt DESC")
    List<DeliveryAnalytics> findByDateAndChannel(@Param("date") LocalDate date,
                                                 @Param("channel") String channel);

    @Query("SELECT da FROM DeliveryAnalytics da WHERE da.sellerId = :sellerId AND da.date BETWEEN :startDate AND :endDate ORDER BY da.date DESC, da.channel")
    List<DeliveryAnalytics> findBySellerIdAndDateRange(
            @Param("sellerId") String sellerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(da.attempted), SUM(da.sent), SUM(da.delivered), SUM(da.failed), SUM(da.bounced), " +
            "SUM(da.opened), SUM(da.clicked) FROM DeliveryAnalytics da " +
            "WHERE da.date = :date AND da.channel = :channel")
    Object[] getAggregatedStatsForDate(@Param("date") LocalDate date,
                                       @Param("channel") String channel);
}