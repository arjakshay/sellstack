package com.stack.sellstack.repository;

import com.stack.sellstack.model.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {

    Optional<Session> findBySessionToken(String sessionToken);

    Optional<Session> findByRefreshTokenHash(String refreshTokenHash);

    List<Session> findBySellerIdAndIsActive(UUID sellerId, Boolean isActive);

    @Query("SELECT s FROM Session s WHERE s.seller.id = :sellerId " +
            "AND s.deviceId = :deviceId " +
            "AND s.isActive = true " +
            "AND s.expiresAt > :now")
    Optional<Session> findActiveSession(
            @Param("sellerId") UUID sellerId,
            @Param("deviceId") String deviceId,
            @Param("now") Instant now);

    @Modifying
    @Query("UPDATE Session s SET s.isActive = false, s.revokedAt = :revokedAt " +
            "WHERE s.seller.id = :sellerId AND s.deviceId = :deviceId")
    void invalidateSession(
            @Param("sellerId") UUID sellerId,
            @Param("deviceId") String deviceId,
            @Param("revokedAt") Instant revokedAt);

    @Modifying
    @Query("UPDATE Session s SET s.isActive = false, s.revokedAt = :revokedAt " +
            "WHERE s.seller.id = :sellerId")
    void invalidateAllSessions(@Param("sellerId") UUID sellerId, @Param("revokedAt") Instant revokedAt);

    @Modifying
    @Query("DELETE FROM Session s WHERE s.expiresAt < :cutoff")
    int deleteExpiredSessions(@Param("cutoff") Instant cutoff);
}