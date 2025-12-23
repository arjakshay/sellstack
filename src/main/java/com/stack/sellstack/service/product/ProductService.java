package com.stack.sellstack.service.product;

import com.stack.sellstack.exception.ValidationException;
import com.stack.sellstack.model.dto.request.ProductRequest;
import com.stack.sellstack.model.dto.response.ProductResponse;
import com.stack.sellstack.exception.BusinessException;
import com.stack.sellstack.exception.ResourceNotFoundException;
import com.stack.sellstack.model.entity.FileMetadata;
import com.stack.sellstack.model.entity.Product;
import com.stack.sellstack.model.entity.Seller;
import com.stack.sellstack.model.enums.ProductStatus;
import com.stack.sellstack.repository.ProductRepository;
import com.stack.sellstack.repository.SellerRepository;
import com.stack.sellstack.service.storage.FileStorageService;
import com.stack.sellstack.util.SlugGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final SellerRepository sellerRepository;
    private final FileStorageService fileStorageService;
    private final SlugGenerator slugGenerator;

    @Transactional
    public ProductResponse createProduct(UUID sellerId, ProductRequest.CreateProductRequest request) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));

        // Validate price
        validatePrice(request.getPrice(), request.getDiscountPrice());

        // Generate unique slug
        String slug = slugGenerator.generateUniqueSlug(request.getTitle());

        // Upload main product file
        FileMetadata productFile = fileStorageService.uploadFile(
                sellerId,
                request.getFile(),
                FileMetadata.FileType.PRODUCT_FILE
        );

        // Upload thumbnail if provided
        FileMetadata thumbnail = null;
        if (request.getThumbnail() != null && !request.getThumbnail().isEmpty()) {
            thumbnail = fileStorageService.uploadFile(
                    sellerId,
                    request.getThumbnail(),
                    FileMetadata.FileType.THUMBNAIL
            );
        }

        // Upload preview if provided
        FileMetadata preview = null;
        if (request.getPreview() != null && !request.getPreview().isEmpty()) {
            preview = fileStorageService.uploadFile(
                    sellerId,
                    request.getPreview(),
                    FileMetadata.FileType.PREVIEW
            );
        }

        // Create product
        Product product = Product.builder()
                .seller(seller)
                .title(request.getTitle())
                .slug(slug)
                .description(request.getDescription())
                .price(request.getPrice())
                .discountPrice(request.getDiscountPrice())
                .currency(request.getCurrency())
                .fileUrl(productFile.getCdnUrl())
                .fileSize(productFile.getFileSize())
                .fileType(productFile.getMimeType())
                .fileName(productFile.getOriginalFilename())
                .fileExtension(productFile.getFileExtension())
                .thumbnailUrl(thumbnail != null ? thumbnail.getCdnUrl() : null)
                .previewUrl(preview != null ? preview.getCdnUrl() : null)
                .galleryUrls(new HashSet<>()) // Can be added later
                .category(request.getCategory())
                .tags(request.getTags() != null ? request.getTags() : new HashSet<>())
                .language(request.getLanguage())
                .status(ProductStatus.DRAFT)
                .allowRefunds(request.getAllowRefunds() != null ? request.getAllowRefunds() : true)
                .refundDays(request.getRefundDays() != null ? request.getRefundDays() : 7)
                .maxDownloads(request.getMaxDownloads() != null ? request.getMaxDownloads() : 3)
                .downloadExpiryDays(request.getDownloadExpiryDays() != null ? request.getDownloadExpiryDays() : 30)
                .metaTitle(request.getMetaTitle())
                .metaDescription(request.getMetaDescription())
                .metaKeywords(request.getMetaKeywords())
                .build();

        product = productRepository.save(product);

        // Update seller product count
        seller.setTotalProducts(seller.getTotalProducts() + 1);
        sellerRepository.save(seller);

        log.info("Product created: {} by seller: {}", product.getId(), sellerId);

        return ProductResponse.fromEntity(product);
    }

    @Transactional
    public ProductResponse updateProduct(UUID sellerId, UUID productId,
                                         ProductRequest.UpdateProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Verify ownership
        if (!product.getSeller().getId().equals(sellerId)) {
            throw new BusinessException("You don't have permission to update this product");
        }

        // Update fields if provided
        if (request.getTitle() != null) {
            product.setTitle(request.getTitle());
            // Regenerate slug if title changed
            String newSlug = slugGenerator.generateUniqueSlug(request.getTitle());
            product.setSlug(newSlug);
        }

        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }

        if (request.getPrice() != null) {
            validatePrice(request.getPrice(), request.getDiscountPrice());
            product.setPrice(request.getPrice());
        }

        if (request.getDiscountPrice() != null) {
            product.setDiscountPrice(request.getDiscountPrice());
        }

        if (request.getCategory() != null) {
            product.setCategory(request.getCategory());
        }

        if (request.getTags() != null) {
            product.setTags(request.getTags());
        }

        if (request.getLanguage() != null) {
            product.setLanguage(request.getLanguage());
        }

        // Update thumbnail if provided
        if (request.getThumbnail() != null && !request.getThumbnail().isEmpty()) {
            // Delete old thumbnail from S3 (optional)
            // Upload new thumbnail
            FileMetadata thumbnail = fileStorageService.uploadFile(
                    sellerId,
                    request.getThumbnail(),
                    FileMetadata.FileType.THUMBNAIL
            );
            product.setThumbnailUrl(thumbnail.getCdnUrl());
        }

        // Update preview if provided
        if (request.getPreview() != null && !request.getPreview().isEmpty()) {
            FileMetadata preview = fileStorageService.uploadFile(
                    sellerId,
                    request.getPreview(),
                    FileMetadata.FileType.PREVIEW
            );
            product.setPreviewUrl(preview.getCdnUrl());
        }

        // Update SEO fields
        if (request.getMetaTitle() != null) {
            product.setMetaTitle(request.getMetaTitle());
        }

        if (request.getMetaDescription() != null) {
            product.setMetaDescription(request.getMetaDescription());
        }

        if (request.getMetaKeywords() != null) {
            product.setMetaKeywords(request.getMetaKeywords());
        }

        product = productRepository.save(product);

        log.info("Product updated: {}", productId);

        return ProductResponse.fromEntity(product);
    }

    @Transactional
    public ProductResponse publishProduct(UUID sellerId, UUID productId, boolean publish) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Verify ownership
        if (!product.getSeller().getId().equals(sellerId)) {
            throw new BusinessException("You don't have permission to update this product");
        }

        if (publish) {
            // Validate product before publishing
            validateProductForPublishing(product);

            product.setStatus(ProductStatus.PUBLISHED);
            product.setPublishedAt(Instant.now());
        } else {
            product.setStatus(ProductStatus.UNPUBLISHED);
        }

        product = productRepository.save(product);

        log.info("Product {}: {}", productId, publish ? "published" : "unpublished");

        return ProductResponse.fromEntity(product);
    }

    @Transactional
    public void deleteProduct(UUID sellerId, UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Verify ownership
        if (!product.getSeller().getId().equals(sellerId)) {
            throw new BusinessException("You don't have permission to delete this product");
        }

        // Soft delete
        product.setStatus(ProductStatus.DELETED);
        product.setIsDeleted(true);
        productRepository.save(product);

        // Update seller product count
        Seller seller = product.getSeller();
        seller.setTotalProducts(Math.max(0, seller.getTotalProducts() - 1));
        sellerRepository.save(seller);

        log.info("Product deleted: {}", productId);

        // Note: We don't delete files from S3 immediately for recovery purposes
        // Actual file deletion can be done via lifecycle policy
    }

    private void validatePrice(BigDecimal price, BigDecimal discountPrice) {
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Price must be greater than 0");
        }

        if (discountPrice != null) {
            if (discountPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Discount price must be greater than 0");
            }

            if (discountPrice.compareTo(price) >= 0) {
                throw new ValidationException("Discount price must be less than regular price");
            }
        }
    }

    private void validateProductForPublishing(Product product) {
        if (product.getFileUrl() == null || product.getFileUrl().isEmpty()) {
            throw new BusinessException("Product file is required for publishing");
        }

        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Valid price is required for publishing");
        }

        if (product.getTitle() == null || product.getTitle().trim().isEmpty()) {
            throw new BusinessException("Title is required for publishing");
        }

        // Check if file is ready (virus scan passed)
        // This would require checking file metadata status
    }

    public Page<ProductResponse> getSellerProducts(UUID sellerId, String status, Pageable pageable) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));

        Page<Product> products;

        if (status != null && !status.trim().isEmpty()) {
            try {
                ProductStatus productStatus = ProductStatus.valueOf(status.toUpperCase());
                products = productRepository.findBySellerIdAndStatus(sellerId, productStatus, pageable);
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Invalid status value: " + status);
            }
        } else {
            products = productRepository.findBySellerId(sellerId, pageable);
        }

        return products.map(ProductResponse::fromEntity);
    }

    /**
     * Get product by ID
     */
    public ProductResponse getProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Increment page views (async)
        incrementPageViews(productId);

        return ProductResponse.fromEntity(product);
    }

    /**
     * Get product by slug
     */
    public ProductResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Check if product is published (unless seller is viewing)
        if (!ProductStatus.PUBLISHED.equals(product.getStatus())) {
            throw new ResourceNotFoundException("Product not found");
        }

        // Increment page views (async)
        incrementPageViews(product.getId());

        return ProductResponse.fromEntity(product);
    }

    /**
     * Browse published products with filters
     */
    public Page<ProductResponse> browseProducts(String category, String search,
                                                BigDecimal minPrice, BigDecimal maxPrice,
                                                Pageable pageable) {
        Page<Product> products;

        if (search != null && !search.trim().isEmpty()) {
            // Search in published products
            products = productRepository.searchPublishedProducts(search.trim(), pageable);
        } else if (category != null && !category.trim().isEmpty()) {
            // Filter by category
            products = productRepository.findByCategoryAndStatus(
                    category.trim(),
                    ProductStatus.PUBLISHED,
                    pageable
            );
        } else {
            // Get all published products
            products = productRepository.findByStatus(ProductStatus.PUBLISHED, pageable);
        }

        // Apply price filters if provided
        if (minPrice != null || maxPrice != null) {
            products = (Page<Product>) products.filter(product -> {
                BigDecimal price = product.getDiscountPrice() != null ?
                        product.getDiscountPrice() : product.getPrice();

                boolean minCondition = minPrice == null || price.compareTo(minPrice) >= 0;
                boolean maxCondition = maxPrice == null || price.compareTo(maxPrice) <= 0;

                return minCondition && maxCondition;
            });
        }

        return products.map(ProductResponse::fromEntity);
    }

    /**
     * Get product for seller (with ownership check)
     */
    public ProductResponse getProductForSeller(UUID sellerId, UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Verify ownership
        if (!product.getSeller().getId().equals(sellerId)) {
            throw new BusinessException("You don't have permission to view this product");
        }

        return ProductResponse.fromEntity(product);
    }

    /**
     * Get product analytics
     */
    public ProductAnalyticsResponse getProductAnalytics(UUID sellerId, UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Verify ownership
        if (!product.getSeller().getId().equals(sellerId)) {
            throw new BusinessException("You don't have permission to view analytics");
        }

        // Calculate conversion rate
        double conversionRate = product.getPageViews() > 0 ?
                (double) product.getSalesCount() / product.getPageViews() * 100 : 0;

        return ProductAnalyticsResponse.builder()
                .productId(productId)
                .pageViews(product.getPageViews())
                .salesCount(product.getSalesCount())
                .downloadCount(product.getDownloadCount())
                .reviewCount(product.getReviewCount())
                .conversionRate(Math.round(conversionRate * 100.0) / 100.0)
                .totalRevenue(calculateProductRevenue(productId))
                .averageRating(product.getRatingAvg())
                .build();
    }

    /**
     * Add gallery images to product
     */
    @Transactional
    public ProductResponse addGalleryImage(UUID sellerId, UUID productId, MultipartFile image) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Verify ownership
        if (!product.getSeller().getId().equals(sellerId)) {
            throw new BusinessException("You don't have permission to update this product");
        }

        // Upload gallery image
        FileMetadata galleryImage = fileStorageService.uploadFile(
                sellerId,
                image,
                FileMetadata.FileType.GALLERY_IMAGE
        );

        // Add to gallery URLs
        if (product.getGalleryUrls() == null) {
            product.setGalleryUrls(new HashSet<>());
        }
        product.getGalleryUrls().add(galleryImage.getCdnUrl());

        product = productRepository.save(product);

        return ProductResponse.fromEntity(product);
    }

    /**
     * Remove gallery image from product
     */
    @Transactional
    public ProductResponse removeGalleryImage(UUID sellerId, UUID productId, String imageUrl) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Verify ownership
        if (!product.getSeller().getId().equals(sellerId)) {
            throw new BusinessException("You don't have permission to update this product");
        }

        // Remove from gallery URLs
        if (product.getGalleryUrls() != null) {
            product.getGalleryUrls().remove(imageUrl);
            product = productRepository.save(product);
        }

        return ProductResponse.fromEntity(product);
    }

    /**
     * Feature/unfeature product
     */
    @Transactional
    public ProductResponse featureProduct(UUID sellerId, UUID productId, boolean featured) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Verify ownership
        if (!product.getSeller().getId().equals(sellerId)) {
            throw new BusinessException("You don't have permission to update this product");
        }

        product.setIsFeatured(featured);
        product = productRepository.save(product);

        log.info("Product {} featured: {}", productId, featured);

        return ProductResponse.fromEntity(product);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Async increment page views
     */
    @Transactional
    public void incrementPageViews(UUID productId) {
        try {
            productRepository.incrementPageViews(productId);
        } catch (Exception e) {
            log.error("Failed to increment page views for product: {}", productId, e);
        }
    }

    /**
     * Calculate product revenue
     */
    private BigDecimal calculateProductRevenue(UUID productId) {
        // This would typically involve querying purchases table
        // For now, return placeholder
        return BigDecimal.ZERO;
    }

    // ==================== DTOs ====================

    @lombok.Data
    @lombok.Builder
    public static class ProductAnalyticsResponse {
        private UUID productId;
        private Integer pageViews;
        private Integer salesCount;
        private Integer downloadCount;
        private Integer reviewCount;
        private Double conversionRate;
        private BigDecimal totalRevenue;
        private BigDecimal averageRating;
    }
}