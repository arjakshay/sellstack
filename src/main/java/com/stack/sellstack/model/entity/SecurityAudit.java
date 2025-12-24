package com.stack.sellstack.model.entity;

import com.stack.sellstack.model.enums.SecurityEventType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "security_audit",
        indexes = {
                @Index(name = "idx_audit_username", columnList = "username"),
                @Index(name = "idx_audit_event_timestamp", columnList = "event_timestamp"),
                @Index(name = "idx_audit_event_type", columnList = "event_type"),
                @Index(name = "idx_audit_ip_address", columnList = "ip_address"),
                @Index(name = "idx_audit_seller_id", columnList = "seller_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private SecurityEventType eventType;

    @Column(name = "username")
    private String username;

    @Column(name = "seller_id")
    private UUID sellerId;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "event_timestamp", nullable = false, updatable = false)
    private Instant eventTimestamp;

    @Column(name = "is_success", nullable = false)
    private Boolean isSuccess;

    @Column(name = "details", length = 5000)
    private String details;

    @Column(name = "severity")
    private String severity; // LOW, MEDIUM, HIGH

    @PrePersist
    private void setDefaults() {
        if (isSuccess == null) {
            isSuccess = false;
        }
        if (eventTimestamp == null) {
            eventTimestamp = Instant.now();
        }
    }
}