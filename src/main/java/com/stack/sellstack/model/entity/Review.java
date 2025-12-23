package com.stack.sellstack.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.stack.sellstack.model.entity.base.BaseAuditEntity;
import com.stack.sellstack.model.enums.ReviewStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "reviews",
        indexes = {
                @Index(name = "idx_reviews_product_id", columnList = "product_id"),
                @Index(name = "idx_reviews_seller_id", columnList = "seller_id"),
                @Index(name = "idx_reviews_buyer_id", columnList = "buyer_id"),
                @Index(name = "idx_reviews_rating", columnList = "rating"),
                @Index(name = "idx_reviews_status", columnList = "status"),
                @Index(name = "idx_reviews_created_at", columnList = "created_at DESC")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reviews_product"))
    private Product product;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reviews_seller"))
    private Seller seller;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reviews_buyer"))
    private Buyer buyer;

    @Column(name = "rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "positive_feedback", columnDefinition = "jsonb")
    private Set<String> positiveFeedback = new HashSet<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "negative_feedback", columnDefinition = "jsonb")
    private Set<String> negativeFeedback = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ReviewStatus status;

    @Column(name = "is_verified_purchase")
    private Boolean isVerifiedPurchase;

    @Column(name = "likes_count")
    private Integer likesCount;

    @Column(name = "reports_count")
    private Integer reportsCount;

    @Column(name = "helpful_count")
    private Integer helpfulCount;

    @Column(name = "not_helpful_count")
    private Integer notHelpfulCount;

    @Column(name = "replied_at")
    private java.time.Instant repliedAt;

    @Column(name = "reply_text", columnDefinition = "TEXT")
    private String replyText;

    @PrePersist
    @PreUpdate
    private void setDefaults() {
        if (rating == null) {
            rating = BigDecimal.ZERO;
        }
        if (status == null) {
            status = ReviewStatus.PENDING;
        }
        if (isVerifiedPurchase == null) {
            isVerifiedPurchase = false;
        }
        if (likesCount == null) {
            likesCount = 0;
        }
        if (reportsCount == null) {
            reportsCount = 0;
        }
        if (helpfulCount == null) {
            helpfulCount = 0;
        }
        if (notHelpfulCount == null) {
            notHelpfulCount = 0;
        }
    }
}