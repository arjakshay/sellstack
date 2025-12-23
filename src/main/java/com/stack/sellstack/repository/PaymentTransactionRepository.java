package com.stack.sellstack.repository;

import com.stack.sellstack.model.entity.PaymentTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    List<PaymentTransaction> findBySellerId(UUID sellerId);

    List<PaymentTransaction> findByPaymentId(UUID paymentId);

    List<PaymentTransaction> findByType(String type);

    List<PaymentTransaction> findByStatus(String status);

    @Query("SELECT t FROM PaymentTransaction t WHERE t.seller.id = :sellerId AND t.type = :type AND t.status = :status")
    Page<PaymentTransaction> findBySellerIdAndTypeAndStatus(
            @Param("sellerId") UUID sellerId,
            @Param("type") String type,
            @Param("status") String status,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PaymentTransaction t WHERE t.seller.id = :sellerId AND t.type = 'CREDIT' AND t.status = 'COMPLETED'")
    BigDecimal sumCreditsBySeller(@Param("sellerId") UUID sellerId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PaymentTransaction t WHERE t.seller.id = :sellerId AND t.type = 'DEBIT' AND t.status = 'COMPLETED'")
    BigDecimal sumDebitsBySeller(@Param("sellerId") UUID sellerId);

    @Query("SELECT t FROM PaymentTransaction t WHERE t.seller.id = :sellerId AND t.createdAt BETWEEN :startDate AND :endDate")
    List<PaymentTransaction> findBySellerIdAndDateRange(
            @Param("sellerId") UUID sellerId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);
}