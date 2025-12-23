package com.stack.sellstack.repository;

import com.stack.sellstack.model.entity.PayoutRequest;
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
public interface PayoutRequestRepository extends JpaRepository<PayoutRequest, UUID> {

    List<PayoutRequest> findBySellerId(UUID sellerId);

    List<PayoutRequest> findByStatus(String status);

    Optional<PayoutRequest> findByRazorpayPayoutId(String razorpayPayoutId);

    @Query("SELECT pr FROM PayoutRequest pr WHERE pr.seller.id = :sellerId AND pr.status IN :statuses")
    Page<PayoutRequest> findBySellerIdAndStatusIn(
            @Param("sellerId") UUID sellerId,
            @Param("statuses") List<String> statuses,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(pr.amount), 0) FROM PayoutRequest pr WHERE pr.seller.id = :sellerId AND pr.status = 'COMPLETED' AND pr.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumCompletedPayoutsBySellerAndDateRange(
            @Param("sellerId") UUID sellerId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query("SELECT COUNT(pr) FROM PayoutRequest pr WHERE pr.seller.id = :sellerId AND pr.status = 'PENDING'")
    Long countPendingPayoutsBySeller(@Param("sellerId") UUID sellerId);
}