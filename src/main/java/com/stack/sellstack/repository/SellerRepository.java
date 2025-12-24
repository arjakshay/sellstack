package com.stack.sellstack.repository;

import com.stack.sellstack.model.entity.Seller;
import com.stack.sellstack.model.enums.SellerStatus;
import com.stack.sellstack.model.enums.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerRepository extends JpaRepository<Seller, UUID> {

    Optional<Seller> findByEmail(String email);
    Optional<Seller> findByPhone(String phone);
    Optional<Seller> findByEmailOrPhone(String email, String phone);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    @Query("SELECT s FROM Seller s WHERE s.email = :identifier OR s.phone = :identifier")
    Optional<Seller> findByUsername(@Param("identifier") String identifier);

    @Query("SELECT s FROM Seller s WHERE LOWER(REPLACE(s.displayName, ' ', '-')) = LOWER(:slug)")
    Optional<Seller> findByDisplayNameSlug(@Param("slug") String slug);

    @Query("SELECT s FROM Seller s WHERE " +
            "(:status IS NULL OR s.status = :status) AND " +
            "(:verificationStatus IS NULL OR s.verificationStatus = :verificationStatus) AND " +
            "(:createdAfter IS NULL OR s.createdAt >= :createdAfter) " +
            "ORDER BY s.createdAt DESC")
    Page<Seller> findAllWithFilters(
            @Param("status") SellerStatus status,
            @Param("verificationStatus") VerificationStatus verificationStatus,
            @Param("createdAfter") LocalDateTime createdAfter,
            Pageable pageable);

    @Modifying
    @Query("UPDATE Seller s SET s.lastLoginAt = :loginTime WHERE s.id = :id")
    void updateLastLogin(@Param("id") UUID id, @Param("loginTime") Instant loginTime);

    @Modifying
    @Query("UPDATE Seller s SET s.availableBalance = s.availableBalance + :amount, " +
            "s.totalEarnings = s.totalEarnings + :amount WHERE s.id = :id")
    void addToBalance(@Param("id") UUID id, @Param("amount") BigDecimal amount);

    @Modifying
    @Query("UPDATE Seller s SET s.totalSales = s.totalSales + 1, " +
            "s.totalEarnings = s.totalEarnings + :amount, " +
            "s.availableBalance = s.availableBalance + :amount WHERE s.id = :id")
    void incrementTotalSales(@Param("id") UUID id, @Param("amount") BigDecimal amount);

    @Modifying
    @Query("UPDATE Seller s SET s.totalProducts = s.totalProducts + 1 WHERE s.id = :id")
    void incrementTotalProducts(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Seller s SET s.ratingAvg = (:currentRating * s.ratingCount + :newRating) / (s.ratingCount + 1), " +
            "s.ratingCount = s.ratingCount + 1 WHERE s.id = :id")
    void updateRating(@Param("id") UUID id, @Param("newRating") BigDecimal newRating);
}