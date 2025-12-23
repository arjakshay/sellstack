package com.stack.sellstack.model.dto.response;

import com.stack.sellstack.model.entity.Product;
import com.stack.sellstack.model.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private UUID id;
    private UUID sellerId;
    private String sellerName;
    private String title;
    private String slug;
    private String description;
    private String shortDescription;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private String currency;
    private String fileUrl;
    private Long fileSize;
    private String fileType;
    private String fileName;
    private String thumbnailUrl;
    private String previewUrl;
    private Set<String> galleryUrls;
    private String category;
    private Set<String> tags;
    private String language;
    private Integer pageViews;
    private Integer salesCount;
    private Integer downloadCount;
    private Integer reviewCount;
    private BigDecimal ratingAvg;
    private ProductStatus status;
    private Boolean isFeatured;
    private Boolean allowRefunds;
    private Integer refundDays;
    private Integer maxDownloads;
    private Integer downloadExpiryDays;
    private String metaTitle;
    private String metaDescription;
    private String metaKeywords;
    private Instant publishedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static ProductResponse fromEntity(Product product) {
        if (product == null) return null;

        return ProductResponse.builder()
                .id(product.getId())
                .sellerId(product.getSeller() != null ? product.getSeller().getId() : null)
                .sellerName(product.getSeller() != null ? product.getSeller().getDisplayName() : null)
                .title(product.getTitle())
                .slug(product.getSlug())
                .description(product.getDescription())
                .shortDescription(product.getShortDescription())
                .price(product.getPrice())
                .discountPrice(product.getDiscountPrice())
                .currency(product.getCurrency())
                .fileUrl(product.getFileUrl())
                .fileSize(product.getFileSize())
                .fileType(product.getFileType())
                .fileName(product.getFileName())
                .thumbnailUrl(product.getThumbnailUrl())
                .previewUrl(product.getPreviewUrl())
                .galleryUrls(product.getGalleryUrls())
                .category(product.getCategory())
                .tags(product.getTags())
                .language(product.getLanguage())
                .pageViews(product.getPageViews())
                .salesCount(product.getSalesCount())
                .downloadCount(product.getDownloadCount())
                .reviewCount(product.getReviewCount())
                .ratingAvg(product.getRatingAvg())
                .status(product.getStatus())
                .isFeatured(product.getIsFeatured())
                .allowRefunds(product.getAllowRefunds())
                .refundDays(product.getRefundDays())
                .maxDownloads(product.getMaxDownloads())
                .downloadExpiryDays(product.getDownloadExpiryDays())
                .metaTitle(product.getMetaTitle())
                .metaDescription(product.getMetaDescription())
                .metaKeywords(product.getMetaKeywords())
                .publishedAt(product.getPublishedAt())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}