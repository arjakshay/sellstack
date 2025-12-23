package com.stack.sellstack.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.stack.sellstack.model.enums.DeviceType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sessions",
        indexes = {
                @Index(name = "idx_sessions_seller_id", columnList = "seller_id, is_active"),
                @Index(name = "idx_sessions_expires_at", columnList = "expires_at"),
                @Index(name = "idx_sessions_session_token", columnList = "session_token"),
                @Index(name = "idx_sessions_refresh_token_hash", columnList = "refresh_token_hash")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false, foreignKey = @ForeignKey(name = "fk_sessions_seller"))
    private Seller seller;

    @Column(name = "session_token", nullable = false, length = 500)
    private String sessionToken;

    @Column(name = "refresh_token_hash", nullable = false, length = 255)
    private String refreshTokenHash;

    @Column(name = "device_id", length = 255)
    private String deviceId;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", length = 50)
    private DeviceType deviceType;

    @Column(name = "os_name", length = 50)
    private String osName;

    @Column(name = "browser_name", length = 50)
    private String browserName;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "country_code", length = 10)
    private String countryCode;

    @Column(name = "region_name", length = 100)
    private String regionName;

    @Column(name = "city_name", length = 100)
    private String cityName;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "is_suspicious", nullable = false)
    private Boolean isSuspicious;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_accessed_at", nullable = false)
    private Instant lastAccessedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @PrePersist
    private void setDefaults() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (lastAccessedAt == null) {
            lastAccessedAt = Instant.now();
        }
        if (isActive == null) {
            isActive = true;
        }
        if (isSuspicious == null) {
            isSuspicious = false;
        }
    }
}