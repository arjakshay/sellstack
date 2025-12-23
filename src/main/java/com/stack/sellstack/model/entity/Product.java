package com.stack.sellstack.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.stack.sellstack.model.entity.base.BaseAuditEntity;
import com.stack.sellstack.model.enums.ProductStatus;
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
@Table(name = "products",
        indexes = {
                @Index(name = "idx_products_seller_id", columnList = "seller_id"),
                @Index(name = "idx_products_slug", columnList = "slug"),
                @Index(name = "idx_products_status", columnList = "status"),
                @Index(name = "idx_products_category", columnList = "category"),
                @Index(name = "idx_products_created_at", columnList = "created_at DESC"),
                @Index(name = "idx_products_price_range", columnList = "price")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_products_slug", columnNames = "slug")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false, foreignKey = @ForeignKey(name = "fk_products_seller"))
    private Seller seller;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "slug", nullable = false, length = 255)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "discount_price", precision = 10, scale = 2)
    private BigDecimal discountPrice;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "file_url", length = 1000)
    private String fileUrl;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_type", length = 100)
    private String fileType;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_extension", length = 10)
    private String fileExtension;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @Column(name = "preview_url", length = 1000)
    private String previewUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "gallery_urls", columnDefinition = "jsonb")
    private Set<String> galleryUrls = new HashSet<>();

    @Column(name = "category", length = 100)
    private String category;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "jsonb")
    private Set<String> tags = new HashSet<>();

    @Column(name = "language", length = 50)
    private String language;

    @Column(name = "page_views")
    private Integer pageViews;

    @Column(name = "sales_count")
    private Integer salesCount;

    @Column(name = "download_count")
    private Integer downloadCount;

    @Column(name = "review_count")
    private Integer reviewCount;

    @Column(name = "rating_avg", precision = 3, scale = 2)
    private BigDecimal ratingAvg;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ProductStatus status;

    @Column(name = "is_featured")
    private Boolean isFeatured;

    @Column(name = "allow_refunds")
    private Boolean allowRefunds;

    @Column(name = "refund_days")
    private Integer refundDays;

    @Column(name = "max_downloads")
    private Integer maxDownloads;

    @Column(name = "download_expiry_days")
    private Integer downloadExpiryDays;

    @Column(name = "meta_title", length = 255)
    private String metaTitle;

    @Column(name = "meta_description", length = 500)
    private String metaDescription;

    @Column(name = "meta_keywords", length = 500)
    private String metaKeywords;

    @Column(name = "published_at")
    private Instant publishedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Purchase> purchases = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Review> reviews = new HashSet<>();

    @PrePersist
    @PreUpdate
    private void setDefaults() {
        if (currency == null) {
            currency = "INR";
        }
        if (status == null) {
            status = ProductStatus.DRAFT;
        }
        if (pageViews == null) {
            pageViews = 0;
        }
        if (salesCount == null) {
            salesCount = 0;
        }
        if (downloadCount == null) {
            downloadCount = 0;
        }
        if (reviewCount == null) {
            reviewCount = 0;
        }
        if (isFeatured == null) {
            isFeatured = false;
        }
        if (allowRefunds == null) {
            allowRefunds = true;
        }
        if (refundDays == null) {
            refundDays = 7;
        }
        if (maxDownloads == null) {
            maxDownloads = 3;
        }
        if (downloadExpiryDays == null) {
            downloadExpiryDays = 30;
        }
        if (language == null) {
            language = "English";
        }
    }
}