package com.stack.sellstack.repository;

import com.stack.sellstack.model.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefundRepository extends JpaRepository<Refund, UUID> {

    Optional<Refund> findByRazorpayRefundId(String razorpayRefundId);

    List<Refund> findByPaymentId(UUID paymentId);

    List<Refund> findByStatus(String status);

    List<Refund> findByInitiatedById(UUID sellerId);

    @Query("SELECT COUNT(r) FROM Refund r WHERE r.payment.id = :paymentId")
    Long countByPaymentId(@Param("paymentId") UUID paymentId);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.payment.id = :paymentId AND r.status = 'PROCESSED'")
    BigDecimal sumProcessedRefundsByPayment(@Param("paymentId") UUID paymentId);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.payment.seller.id = :sellerId AND r.status = 'PROCESSED'")
    BigDecimal sumProcessedRefundsBySeller(@Param("sellerId") UUID sellerId);
}