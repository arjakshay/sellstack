package com.stack.sellstack.repository;

import com.stack.sellstack.model.entity.OtpStorage;
import com.stack.sellstack.model.enums.OTPType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpStorageRepository extends JpaRepository<OtpStorage, UUID> {

    @Query("SELECT o FROM OtpStorage o WHERE o.phone = :phone " +
            "AND o.otpType = :otpType " +
            "AND o.expiresAt > :now " +
            "AND o.verifiedAt IS NULL " +
            "ORDER BY o.createdAt DESC LIMIT 1")
    Optional<OtpStorage> findRecentUnverifiedOtp(
            @Param("phone") String phone,
            @Param("otpType") OTPType otpType,
            @Param("now") Instant now);

    @Query("SELECT o FROM OtpStorage o WHERE o.phone = :phone " +
            "AND o.otpType = :otpType " +
            "AND o.expiresAt > :now " +
            "AND o.verifiedAt IS NULL " +
            "ORDER BY o.createdAt DESC LIMIT 1")
    Optional<OtpStorage> findValidOtp(
            @Param("phone") String phone,
            @Param("otpType") OTPType otpType,
            @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM OtpStorage o WHERE o.expiresAt < :cutoff")
    int deleteExpiredOtps(@Param("cutoff") Instant cutoff);

    @Query("SELECT COUNT(o) FROM OtpStorage o WHERE o.phone = :phone " +
            "AND o.createdAt >= :since")
    int countOtpsSince(@Param("phone") String phone, @Param("since") Instant since);
}