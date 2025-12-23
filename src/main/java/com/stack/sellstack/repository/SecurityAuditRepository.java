package com.stack.sellstack.repository;

import com.stack.sellstack.model.entity.SecurityAudit;
import com.stack.sellstack.model.enums.SecurityEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SecurityAuditRepository extends JpaRepository<SecurityAudit, Long> {

    // Use the correct field name from your entity
    List<SecurityAudit> findByUsernameOrderByEventTimestampDesc(String username);

    // Use Instant instead of LocalDateTime
    List<SecurityAudit> findByEventTimestampAfterAndSeverityNotNullOrderByEventTimestampDesc(Instant timestamp);

    // Update query to use Instant and correct field names
    @Query("SELECT COUNT(sa) FROM SecurityAudit sa WHERE " +
            "(sa.username = :username OR sa.ipAddress = :ipAddress) " +
            "AND sa.eventType = com.stack.sellstack.model.enums.SecurityEventType.LOGIN_FAILED " +
            "AND sa.eventTimestamp >= :since")
    int countFailedLoginAttempts(@Param("username") String username,
                                 @Param("ipAddress") String ipAddress,
                                 @Param("since") Instant since);

    // Add this method for getting recent events
    @Query("SELECT sa FROM SecurityAudit sa " +
            "WHERE sa.eventTimestamp >= :since " +
            "ORDER BY sa.eventTimestamp DESC")
    List<SecurityAudit> findRecentEvents(@Param("since") Instant since);

    // Optional: Add pagination version
    @Query("SELECT sa FROM SecurityAudit sa " +
            "WHERE sa.eventTimestamp >= :since " +
            "ORDER BY sa.eventTimestamp DESC")
    List<SecurityAudit> findRecentEvents(@Param("since") Instant since,
                                         org.springframework.data.domain.Pageable pageable);
}