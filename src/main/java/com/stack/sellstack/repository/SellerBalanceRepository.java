package com.stack.sellstack.repository;

import com.stack.sellstack.model.entity.SellerBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerBalanceRepository extends JpaRepository<SellerBalance, UUID> {

    Optional<SellerBalance> findBySellerId(UUID sellerId);

    @Modifying
    @Query("UPDATE SellerBalance sb SET sb.availableBalance = sb.availableBalance + :amount, sb.totalEarnings = sb.totalEarnings + :amount, sb.updatedAt = CURRENT_TIMESTAMP WHERE sb.seller.id = :sellerId")
    int addToBalance(@Param("sellerId") UUID sellerId, @Param("amount") BigDecimal amount);

    @Modifying
    @Query("UPDATE SellerBalance sb SET sb.pendingBalance = sb.pendingBalance + :amount, sb.updatedAt = CURRENT_TIMESTAMP WHERE sb.seller.id = :sellerId")
    int addToPending(@Param("sellerId") UUID sellerId, @Param("amount") BigDecimal amount);

    @Modifying
    @Query("UPDATE SellerBalance sb SET sb.pendingBalance = sb.pendingBalance - :amount, sb.availableBalance = sb.availableBalance + :amount, sb.updatedAt = CURRENT_TIMESTAMP WHERE sb.seller.id = :sellerId AND sb.pendingBalance >= :amount")
    int movePendingToAvailable(@Param("sellerId") UUID sellerId, @Param("amount") BigDecimal amount);

    @Modifying
    @Query("UPDATE SellerBalance sb SET sb.availableBalance = sb.availableBalance - :amount, sb.updatedAt = CURRENT_TIMESTAMP WHERE sb.seller.id = :sellerId AND sb.availableBalance >= :amount")
    int deductFromBalance(@Param("sellerId") UUID sellerId, @Param("amount") BigDecimal amount);
}