package com.stack.sellstack.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.stack.sellstack.model.entity.base.BaseAuditEntity;
import com.stack.sellstack.model.enums.Role;
import com.stack.sellstack.model.enums.SellerStatus;
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
@Table(name = "sellers",
        indexes = {
                @Index(name = "idx_sellers_email", columnList = "email"),
                @Index(name = "idx_sellers_phone", columnList = "phone"),
                @Index(name = "idx_sellers_status", columnList = "status"),
                @Index(name = "idx_sellers_created_at", columnList = "created_at DESC")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_sellers_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_sellers_phone", columnNames = "phone")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Seller extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "phone", nullable = false, length = 20)
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

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private SellerStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 50)
    private VerificationStatus verificationStatus;

    @Column(name = "upi_id", length = 100)
    private String upiId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "bank_details", columnDefinition = "jsonb")
    private BankDetails bankDetails;

    @Column(name = "gst_number", length = 50)
    private String gstNumber;

    @Column(name = "pan_number", length = 50)
    private String panNumber;

    @Column(name = "total_earnings", precision = 15, scale = 2)
    private BigDecimal totalEarnings;

    @Column(name = "available_balance", precision = 15, scale = 2)
    private BigDecimal availableBalance;

    @Column(name = "total_sales")
    private Integer totalSales;

    @Column(name = "total_products")
    private Integer totalProducts;

    @Column(name = "rating_avg", precision = 3, scale = 2)
    private BigDecimal ratingAvg;

    @Column(name = "rating_count")
    private Integer ratingCount;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "phone_verified_at")
    private Instant phoneVerifiedAt;

    @Column(name = "marketing_consent", nullable = false)
    private Boolean marketingConsent;

    @Column(name = "terms_accepted_at", nullable = false)
    private Instant termsAcceptedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Product> products = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Session> sessions = new HashSet<>();

    public boolean isEnabled() {
        return this.status == SellerStatus.ACTIVE;
    }

    // Add a roles field or method
    public Set<Role> getRoles() {
        // If you have a roles field, return it
        // If not, return a default set
        return Set.of(Role.SELLER);
    }

    @PrePersist
    @PreUpdate
    private void setDefaults() {
        if (status == null) {
            status = SellerStatus.ACTIVE;
        }
        if (verificationStatus == null) {
            verificationStatus = VerificationStatus.PENDING;
        }
        if (totalEarnings == null) {
            totalEarnings = BigDecimal.ZERO;
        }
        if (availableBalance == null) {
            availableBalance = BigDecimal.ZERO;
        }
        if (totalSales == null) {
            totalSales = 0;
        }
        if (totalProducts == null) {
            totalProducts = 0;
        }
        if (ratingCount == null) {
            ratingCount = 0;
        }
        if (marketingConsent == null) {
            marketingConsent = false;
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BankDetails {
        private String accountHolderName;
        private String accountNumber;
        private String ifscCode;
        private String bankName;
        private String branchName;
        private String accountType; // SAVINGS, CURRENT
    }
}