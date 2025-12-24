package com.stack.sellstack.repository;

import com.stack.sellstack.model.entity.BalanceTransaction;
import com.stack.sellstack.model.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface BalanceTransactionRepository extends JpaRepository<BalanceTransaction, UUID> {

    @Query("SELECT bt FROM BalanceTransaction bt WHERE bt.sellerId = :sellerId " +
            "AND (:type IS NULL OR bt.type = :type) " +
            "AND (:startDate IS NULL OR CAST(bt.createdAt AS date) >= :startDate) " +
            "AND (:endDate IS NULL OR CAST(bt.createdAt AS date) <= :endDate) " +
            "ORDER BY bt.createdAt DESC")
    Page<BalanceTransaction> findBySellerId(@Param("sellerId") UUID sellerId,
                                            @Param("type") TransactionType type,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate,
                                            Pageable pageable);

    @Modifying
    @Query("UPDATE BalanceTransaction bt SET bt.status = com.stack.sellstack.model.enums.TransactionStatus.COMPLETED " +
            "WHERE bt.reference = :reference AND bt.status = com.stack.sellstack.model.enums.TransactionStatus.PENDING")
    int markAsCompleted(@Param("reference") String reference);
}