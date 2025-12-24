package com.stack.sellstack.repository;

import com.stack.sellstack.model.entity.SellerAnalytics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface SellerAnalyticsRepository extends JpaRepository<SellerAnalytics, UUID> {

    @Query(value = """
        SELECT COALESCE(SUM(p.amount), 0) 
        FROM purchases p 
        WHERE p.seller_id = :sellerId 
        AND p.created_at BETWEEN :startDate AND :endDate 
        AND p.status = 'COMPLETED'
        """, nativeQuery = true)
    BigDecimal getTotalRevenue(@Param("sellerId") UUID sellerId,
                               @Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);

    @Query(value = """
        SELECT COUNT(*) 
        FROM purchases p 
        WHERE p.seller_id = :sellerId 
        AND p.created_at BETWEEN :startDate AND :endDate 
        AND p.status = 'COMPLETED'
        """, nativeQuery = true)
    Integer getTotalSales(@Param("sellerId") UUID sellerId,
                          @Param("startDate") LocalDateTime startDate,
                          @Param("endDate") LocalDateTime endDate);

    @Query(value = """
        SELECT COUNT(DISTINCT p.id) 
        FROM products p 
        WHERE p.seller_id = :sellerId AND p.is_published = true
        """, nativeQuery = true)
    Integer getActiveProductsCount(@Param("sellerId") UUID sellerId);

    @Query(value = """
        SELECT COUNT(DISTINCT pur.buyer_email) 
        FROM purchases pur 
        WHERE pur.seller_id = :sellerId 
        AND pur.created_at BETWEEN :startDate AND :endDate
        """, nativeQuery = true)
    Integer getUniqueCustomers(@Param("sellerId") UUID sellerId,
                               @Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);

    @Query(value = """
        SELECT COALESCE(SUM(pv.view_count), 0) 
        FROM product_views pv 
        JOIN products p ON pv.product_id = p.id 
        WHERE p.seller_id = :sellerId 
        AND pv.view_date BETWEEN :startDate AND :endDate
        """, nativeQuery = true)
    Integer getTotalProductViews(@Param("sellerId") UUID sellerId,
                                 @Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);

    @Query(value = """
        SELECT MAX(p.created_at) 
        FROM purchases p 
        WHERE p.seller_id = :sellerId
        """, nativeQuery = true)
    LocalDateTime getLastSaleDate(@Param("sellerId") UUID sellerId);

    // Daily analytics queries
    @Query(value = """
        SELECT COUNT(*) 
        FROM purchases pur 
        WHERE pur.seller_id = :sellerId 
        AND pur.created_at BETWEEN :startDate AND :endDate 
        AND pur.status = 'COMPLETED'
        """, nativeQuery = true)
    Integer getDailySalesCount(@Param("sellerId") UUID sellerId,
                               @Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);

    @Query(value = """
        SELECT COALESCE(SUM(pur.amount), 0) 
        FROM purchases pur 
        WHERE pur.seller_id = :sellerId 
        AND pur.created_at BETWEEN :startDate AND :endDate 
        AND pur.status = 'COMPLETED'
        """, nativeQuery = true)
    BigDecimal getDailyRevenue(@Param("sellerId") UUID sellerId,
                               @Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);

    @Query(value = """
        SELECT COUNT(DISTINCT pur.buyer_email) 
        FROM purchases pur 
        WHERE pur.seller_id = :sellerId 
        AND pur.created_at BETWEEN :startDate AND :endDate
        """, nativeQuery = true)
    Integer getDailyNewCustomers(@Param("sellerId") UUID sellerId,
                                 @Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);

    @Query(value = """
        SELECT COALESCE(SUM(pv.view_count), 0) 
        FROM product_views pv 
        JOIN products p ON pv.product_id = p.id 
        WHERE p.seller_id = :sellerId 
        AND DATE(pv.view_date) = DATE(:date)
        """, nativeQuery = true)
    Integer getDailyProductViews(@Param("sellerId") UUID sellerId,
                                 @Param("date") LocalDateTime date);

    // Weekly/Monthly analytics queries (reuse daily methods with different date ranges)
    @Query(value = """
        SELECT COUNT(*) 
        FROM purchases pur 
        WHERE pur.seller_id = :sellerId 
        AND pur.created_at BETWEEN :startDate AND :endDate 
        AND pur.status = 'COMPLETED'
        """, nativeQuery = true)
    Integer getSalesCount(@Param("sellerId") UUID sellerId,
                          @Param("startDate") LocalDateTime startDate,
                          @Param("endDate") LocalDateTime endDate);

    @Query(value = """
        SELECT COALESCE(SUM(pur.amount), 0) 
        FROM purchases pur 
        WHERE pur.seller_id = :sellerId 
        AND pur.created_at BETWEEN :startDate AND :endDate 
        AND pur.status = 'COMPLETED'
        """, nativeQuery = true)
    BigDecimal getRevenue(@Param("sellerId") UUID sellerId,
                          @Param("startDate") LocalDateTime startDate,
                          @Param("endDate") LocalDateTime endDate);

    @Query(value = """
        SELECT COUNT(DISTINCT pur.buyer_email) 
        FROM purchases pur 
        WHERE pur.seller_id = :sellerId 
        AND pur.created_at BETWEEN :startDate AND :endDate
        """, nativeQuery = true)
    Integer getNewCustomers(@Param("sellerId") UUID sellerId,
                            @Param("startDate") LocalDateTime startDate,
                            @Param("endDate") LocalDateTime endDate);

    @Query(value = """
        SELECT COALESCE(SUM(pv.view_count), 0) 
        FROM product_views pv 
        JOIN products p ON pv.product_id = p.id 
        WHERE p.seller_id = :sellerId 
        AND pv.view_date BETWEEN :startDate AND :endDate
        """, nativeQuery = true)
    Integer getProductViews(@Param("sellerId") UUID sellerId,
                            @Param("startDate") LocalDateTime startDate,
                            @Param("endDate") LocalDateTime endDate);

    // Daily analytics pageable query
    @Query(value = """
        SELECT 
            DATE(pur.created_at) as date,
            COUNT(pur.id) as salesCount,
            COALESCE(SUM(pur.amount), 0) as revenue,
            COUNT(DISTINCT pur.buyer_email) as newCustomers,
            COALESCE((
                SELECT SUM(pv.view_count) 
                FROM product_views pv 
                JOIN products p ON pv.product_id = p.id 
                WHERE p.seller_id = :sellerId 
                AND DATE(pv.view_date) = DATE(pur.created_at)
            ), 0) as productViews
        FROM purchases pur
        WHERE pur.seller_id = :sellerId 
        AND pur.status = 'COMPLETED'
        AND DATE(pur.created_at) BETWEEN :startDate AND :endDate
        GROUP BY DATE(pur.created_at)
        ORDER BY DATE(pur.created_at) DESC
        """,
            countQuery = """
        SELECT COUNT(DISTINCT DATE(pur.created_at))
        FROM purchases pur
        WHERE pur.seller_id = :sellerId 
        AND pur.status = 'COMPLETED'
        AND DATE(pur.created_at) BETWEEN :startDate AND :endDate
        """,
            nativeQuery = true)
    Page<Object[]> getDailyAnalyticsData(@Param("sellerId") UUID sellerId,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate,
                                         Pageable pageable);

    @Query(value = """
        SELECT COALESCE(SUM(sp.monthly_price), 0) 
        FROM subscription_products sp 
        WHERE sp.seller_id = :sellerId AND sp.is_active = true
        """, nativeQuery = true)
    BigDecimal getSubscriptionMRR(@Param("sellerId") UUID sellerId);

    @Modifying
    @Transactional
    @Query(value = """
    INSERT INTO product_views (id, product_id, seller_id, view_date, view_count) 
    VALUES (gen_random_uuid(), :productId, :sellerId, CURRENT_DATE, 1)
    ON CONFLICT (product_id, view_date) 
    DO UPDATE SET view_count = product_views.view_count + 1
    """, nativeQuery = true)
    void recordProductView(@Param("productId") UUID productId,
                           @Param("sellerId") UUID sellerId);

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO seller_analytics (id, seller_id, product_id, customer_id, amount, type, created_at) 
        VALUES (gen_random_uuid(), :sellerId, :productId, :customerId, :amount, 'SALE', NOW())
        """, nativeQuery = true)
    void recordSale(@Param("sellerId") UUID sellerId,
                    @Param("productId") UUID productId,
                    @Param("amount") BigDecimal amount,
                    @Param("customerId") UUID customerId);
}