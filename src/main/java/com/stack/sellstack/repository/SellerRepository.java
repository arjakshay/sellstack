package com.stack.sellstack.repository;

import com.stack.sellstack.model.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
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

    @Modifying
    @Query("UPDATE Seller s SET s.lastLoginAt = :loginTime WHERE s.id = :id")
    void updateLastLogin(@Param("id") UUID id, @Param("loginTime") Instant loginTime);

    @Modifying
    @Query("UPDATE Seller s SET s.availableBalance = s.availableBalance + :amount WHERE s.id = :id")
    void addToBalance(@Param("id") UUID id, @Param("amount") java.math.BigDecimal amount);
}