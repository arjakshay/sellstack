package com.stack.sellstack.repository;

import com.stack.sellstack.model.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    Optional<Payment> findByReceiptNumber(String receiptNumber);

    List<Payment> findBySellerId(UUID sellerId);

    List<Payment> findByBuyerId(UUID buyerId);

    List<Payment> findByProductId(UUID productId);

    List<Payment> findByStatus(String status);

    @Query("SELECT p FROM Payment p WHERE p.seller.id = :sellerId AND p.status = :status ORDER BY p.createdAt DESC")
    Page<Payment> findBySellerIdAndStatus(
            @Param("sellerId") UUID sellerId,
            @Param("status") String status,
            Pageable pageable);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.seller.id = :sellerId AND p.status IN ('CAPTURED', 'COMPLETED')")
    BigDecimal sumCapturedAmountBySeller(@Param("sellerId") UUID sellerId);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.seller.id = :sellerId AND p.status IN ('CAPTURED', 'COMPLETED')")
    Long countSuccessfulPaymentsBySeller(@Param("sellerId") UUID sellerId);

    @Query("SELECT p FROM Payment p WHERE p.seller.id = :sellerId AND p.createdAt BETWEEN :startDate AND :endDate")
    List<Payment> findBySellerIdAndDateRange(
            @Param("sellerId") UUID sellerId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    boolean existsByRazorpayOrderId(String razorpayOrderId);

    boolean existsByRazorpayPaymentId(String razorpayPaymentId);

    boolean existsByReceiptNumber(String receiptNumber);

    List<Payment> findAllByRazorpayOrderId(String razorpayOrderId);
}