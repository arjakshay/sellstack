package com.stack.sellstack.repository;

import com.stack.sellstack.model.entity.RateLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface RateLimitRepository extends JpaRepository<RateLimit, Long> {

    @Query("SELECT COUNT(rl) FROM RateLimit rl " +
            "WHERE rl.key = :key AND rl.action = :action AND rl.attemptedAt >= :windowStart")
    int countAttemptsInWindow(@Param("key") String key,
                              @Param("action") String action,
                              @Param("windowStart") Instant windowStart);

    @Modifying
    @Query("DELETE FROM RateLimit rl WHERE rl.attemptedAt < :cutoff")
    int deleteOldRecords(@Param("cutoff") Instant cutoff);

    @Query("SELECT rl FROM RateLimit rl " +
            "WHERE rl.key = :key AND rl.action = :action " +
            "ORDER BY rl.attemptedAt DESC")
    List<RateLimit> findRecentAttempts(@Param("key") String key,
                                       @Param("action") String action);
}