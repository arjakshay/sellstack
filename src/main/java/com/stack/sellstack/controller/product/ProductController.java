package com.stack.sellstack.controller.product;

import com.stack.sellstack.model.dto.request.ProductRequest;
import com.stack.sellstack.model.dto.response.ApiResponse;
import com.stack.sellstack.model.dto.response.ProductResponse;
import com.stack.sellstack.security.CurrentUser;
import com.stack.sellstack.service.product.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Product Management", description = "Product creation and management APIs")
public class ProductController {

    private final ProductService productService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Create a new product",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @CurrentUser UUID sellerId,
            @Valid @ModelAttribute ProductRequest.CreateProductRequest request) {

        log.info("Creating product by seller: {}", sellerId);

        ProductResponse response = productService.createProduct(sellerId, request);

        return ResponseEntity.ok(ApiResponse.success(response, "Product created successfully"));
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Update product",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @CurrentUser UUID sellerId,
            @PathVariable UUID productId,
            @Valid @ModelAttribute ProductRequest.UpdateProductRequest request) {

        log.info("Updating product: {} by seller: {}", productId, sellerId);

        ProductResponse response = productService.updateProduct(sellerId, productId, request);

        return ResponseEntity.ok(ApiResponse.success(response, "Product updated successfully"));
    }

    @PatchMapping("/{productId}/publish")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Publish or unpublish product",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ProductResponse>> publishProduct(
            @CurrentUser UUID sellerId,
            @PathVariable UUID productId,
            @Valid @RequestBody ProductRequest.PublishProductRequest request) {

        log.info("{} product: {} by seller: {}",
                request.getPublish() ? "Publishing" : "Unpublishing",
                productId, sellerId);

        ProductResponse response = productService.publishProduct(sellerId, productId, request.getPublish());

        return ResponseEntity.ok(ApiResponse.success(response,
                request.getPublish() ? "Product published" : "Product unpublished"));
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Delete product",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @CurrentUser UUID sellerId,
            @PathVariable UUID productId) {

        log.info("Deleting product: {} by seller: {}", productId, sellerId);

        productService.deleteProduct(sellerId, productId);

        return ResponseEntity.ok(ApiResponse.success(null, "Product deleted"));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get seller's products",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getMyProducts(
            @CurrentUser UUID sellerId,
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String status) {

        log.info("Getting products for seller: {}", sellerId);

        Page<ProductResponse> response = productService.getSellerProducts(sellerId, status, pageable);

        return ResponseEntity.ok(ApiResponse.success(response, "Products retrieved"));
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get product details")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(
            @PathVariable UUID productId) {

        log.info("Getting product: {}", productId);

        ProductResponse response = productService.getProduct(productId);

        return ResponseEntity.ok(ApiResponse.success(response, "Product retrieved"));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get product by slug")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductBySlug(
            @PathVariable String slug) {

        log.info("Getting product by slug: {}", slug);

        ProductResponse response = productService.getProductBySlug(slug);

        return ResponseEntity.ok(ApiResponse.success(response, "Product retrieved"));
    }

    @GetMapping
    @Operation(summary = "Browse published products")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> browseProducts(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {

        log.info("Browsing products");

        Page<ProductResponse> response = productService.browseProducts(
                category, search, minPrice, maxPrice, pageable);

        return ResponseEntity.ok(ApiResponse.success(response, "Products retrieved"));
    }
}