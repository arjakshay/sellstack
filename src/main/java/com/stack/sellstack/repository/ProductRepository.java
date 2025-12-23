package com.stack.sellstack.repository;

import com.stack.sellstack.model.entity.Product;
import com.stack.sellstack.model.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findBySlug(String slug);

    Page<Product> findBySellerIdAndStatus(UUID sellerId, ProductStatus status, Pageable pageable);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    Page<Product> findByCategoryAndStatus(String category, ProductStatus status, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.status = 'PUBLISHED' AND " +
            "(:search IS NULL OR " +
            "LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.category) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> searchPublishedProducts(@Param("search") String search, Pageable pageable);

    @Modifying
    @Query("UPDATE Product p SET p.pageViews = p.pageViews + 1 WHERE p.id = :id")
    void incrementPageViews(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Product p SET p.salesCount = p.salesCount + 1 WHERE p.id = :id")
    void incrementSalesCount(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Product p SET p.downloadCount = p.downloadCount + 1 WHERE p.id = :id")
    void incrementDownloadCount(@Param("id") UUID id);

    Long countBySellerId(UUID sellerId);
    Page<Product> findBySellerId(UUID sellerId, Pageable pageable);

    /**
     * Find products by tags
     */
    @Query(
            value = """
          SELECT * FROM products p
          WHERE p.tags @> CAST(:tagJson AS jsonb)
          """,
            countQuery = """
          SELECT count(*) FROM products p
          WHERE p.tags @> CAST(:tagJson AS jsonb)
          """,
            nativeQuery = true
    )
    Page<Product> findByTag(@Param("tagJson") String tagJson, Pageable pageable);

    /**
     * Get total revenue for product
     */
    @Query("SELECT SUM(p.amount) FROM Purchase p WHERE p.product.id = :productId AND p.paymentStatus = 'COMPLETED'")
    BigDecimal getTotalRevenue(@Param("productId") UUID productId);

    /**
     * Search products with filters
     */
    @Query("SELECT p FROM Product p WHERE p.status = 'PUBLISHED' AND " +
            "(:category IS NULL OR LOWER(p.category) LIKE LOWER(CONCAT('%', :category, '%'))) AND " +
            "(:search IS NULL OR " +
            "LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Product> searchProductsWithFilters(
            @Param("category") String category,
            @Param("search") String search,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);
}