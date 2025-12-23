package com.stack.sellstack.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.stack.sellstack.model.entity.base.BaseAuditEntity;
import com.stack.sellstack.model.enums.BuyerStatus;
import com.stack.sellstack.model.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "buyers",
        indexes = {
                @Index(name = "idx_buyers_email", columnList = "email"),
                @Index(name = "idx_buyers_phone", columnList = "phone"),
                @Index(name = "idx_buyers_status", columnList = "status"),
                @Index(name = "idx_buyers_created_at", columnList = "created_at DESC")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_buyers_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_buyers_phone", columnNames = "phone")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Buyer extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "profile_pic_url", length = 1000)
    private String profilePicUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private BuyerStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 50)
    private VerificationStatus verificationStatus;

    @Column(name = "total_purchases")
    private Integer totalPurchases;

    @Column(name = "total_spent", precision = 10, scale = 2)
    private BigDecimal totalSpent;

    @Column(name = "wishlist_count")
    private Integer wishlistCount;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "marketing_consent")
    private Boolean marketingConsent;

    @Column(name = "terms_accepted_at")
    private Instant termsAcceptedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "buyer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Purchase> purchases = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "buyer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Review> reviews = new HashSet<>();

    @PrePersist
    @PreUpdate
    private void setDefaults() {
        if (status == null) {
            status = BuyerStatus.ACTIVE;
        }
        if (verificationStatus == null) {
            verificationStatus = VerificationStatus.PENDING;
        }
        if (totalPurchases == null) {
            totalPurchases = 0;
        }
        if (totalSpent == null) {
            totalSpent = BigDecimal.ZERO;
        }
        if (wishlistCount == null) {
            wishlistCount = 0;
        }
        if (marketingConsent == null) {
            marketingConsent = false;
        }
    }
}