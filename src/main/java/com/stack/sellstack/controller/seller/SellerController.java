package com.stack.sellstack.controller.seller;

import com.stack.sellstack.model.dto.request.SellerRequest;
import com.stack.sellstack.model.dto.response.*;
import com.stack.sellstack.model.entity.Seller;
import com.stack.sellstack.model.entity.SellerBalance;
import com.stack.sellstack.model.enums.SellerStatus;
import com.stack.sellstack.model.enums.VerificationStatus;
import com.stack.sellstack.service.SellerAnalyticsService;
import com.stack.sellstack.service.SellerService;
import com.stack.sellstack.service.SellerBalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sellers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Sellers", description = "Seller management and profile APIs")
public class SellerController {

    private final SellerService sellerService;
    private final SellerBalanceService sellerBalanceService;
    private final SellerAnalyticsService sellerAnalyticsService;

    // ==================== SELLER PROFILE ENDPOINTS ====================

    @GetMapping("/me")
    @Operation(summary = "Get current seller profile",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<SellerResponse> getCurrentSeller(
            @AuthenticationPrincipal String username) {

        log.info("Getting current seller profile for: {}", username);

        Seller seller = sellerService.findByUsername(username);
        SellerResponse response = SellerResponse.fromEntity(seller);

        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Seller profile retrieved successfully"
        ).getData());
    }

    @PutMapping("/me")
    @Operation(summary = "Update current seller profile",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<SellerResponse>> updateCurrentSeller(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SellerRequest.UpdateProfileRequest request) {

        log.info("Updating seller profile for: {}", userDetails.getUsername());

        Seller seller = sellerService.updateProfile(userDetails.getUsername(), request);
        SellerResponse response = SellerResponse.fromEntity(seller);

        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Profile updated successfully"
        ));
    }

    @PatchMapping("/me/password")
    @Operation(summary = "Change password",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SellerRequest.ChangePasswordRequest request) {

        log.info("Changing password for seller: {}", userDetails.getUsername());

        sellerService.changePassword(userDetails.getUsername(), request);

        return ResponseEntity.ok(ApiResponse.success(
                null,
                "Password changed successfully"
        ));
    }

    // ==================== SELLER BALANCE ENDPOINTS ====================

    @GetMapping("/me/balance")
    @Operation(summary = "Get seller balance information",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<SellerBalanceResponse>> getSellerBalance(
            @AuthenticationPrincipal UserDetails userDetails) {

        Seller seller = sellerService.findByUsername(userDetails.getUsername());
        SellerBalance balance = sellerBalanceService.getSellerBalance(seller.getId());

        return ResponseEntity.ok(ApiResponse.success(
                SellerBalanceResponse.fromEntity(balance),
                "Balance information retrieved"
        ));
    }

    @GetMapping("/me/balance/transactions")
    @Operation(summary = "Get balance transactions",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Page<BalanceTransactionResponse>>> getBalanceTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Seller seller = sellerService.findByUsername(userDetails.getUsername());
        Page<BalanceTransactionResponse> transactions = sellerBalanceService
                .getTransactions(seller.getId(), type, startDate, endDate, pageable);

        return ResponseEntity.ok(ApiResponse.success(
                transactions,
                "Transactions retrieved successfully"
        ));
    }

    @PostMapping("/me/balance/payout-request")
    @Operation(summary = "Request payout",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PayoutResponse>> requestPayout(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SellerRequest.PayoutRequest request) {

        Seller seller = sellerService.findByUsername(userDetails.getUsername());
        PayoutResponse payout = sellerBalanceService.requestPayout(seller.getId(), request);

        return ResponseEntity.ok(ApiResponse.success(
                payout,
                "Payout request submitted successfully"
        ));
    }

    // ==================== SELLER ANALYTICS ENDPOINTS ====================

    @GetMapping("/me/analytics")
    @Operation(summary = "Get seller analytics",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<SellerAnalyticsResponse>> getSellerAnalytics(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Seller seller = sellerService.findByUsername(userDetails.getUsername());
        SellerAnalyticsResponse analytics = sellerAnalyticsService
                .getSellerAnalytics(seller.getId(), startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(
                analytics,
                "Analytics retrieved successfully"
        ));
    }

    @GetMapping("/me/analytics/daily")
    @Operation(summary = "Get daily analytics",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Page<DailyAnalyticsResponse>>> getDailyAnalytics(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 30) Pageable pageable) {

        Seller seller = sellerService.findByUsername(userDetails.getUsername());
        Page<DailyAnalyticsResponse> analytics = sellerAnalyticsService
                .getDailyAnalytics(seller.getId(), startDate, endDate, pageable);

        return ResponseEntity.ok(ApiResponse.success(
                analytics,
                "Daily analytics retrieved"
        ));
    }

    // ==================== ADMIN ENDPOINTS ====================

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all sellers (Admin only)",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Page<SellerResponse>>> getAllSellers(
            @RequestParam(required = false) SellerStatus status,
            @RequestParam(required = false) VerificationStatus verificationStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAfter,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<Seller> sellers = sellerService.getAllSellers(status, verificationStatus, createdAfter, pageable);
        Page<SellerResponse> response = sellers.map(SellerResponse::fromEntity);

        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Sellers retrieved successfully"
        ));
    }

    @GetMapping("/{sellerId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get seller by ID (Admin only)",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<SellerResponse>> getSellerById(
            @PathVariable UUID sellerId) {

        Seller seller = sellerService.findById(sellerId);

        return ResponseEntity.ok(ApiResponse.success(
                SellerResponse.fromEntity(seller),
                "Seller retrieved successfully"
        ));
    }

    @PutMapping("/{sellerId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update seller status (Admin only)",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<SellerResponse>> updateSellerStatus(
            @PathVariable UUID sellerId,
            @Valid @RequestBody SellerRequest.UpdateStatusRequest request) {

        Seller seller = sellerService.updateStatus(sellerId, request);

        return ResponseEntity.ok(ApiResponse.success(
                SellerResponse.fromEntity(seller),
                "Seller status updated successfully"
        ));
    }

    @PostMapping("/{sellerId}/balance/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Adjust seller balance (Admin only)",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<SellerBalanceResponse>> adjustBalance(
            @PathVariable UUID sellerId,
            @Valid @RequestBody SellerRequest.AdjustBalanceRequest request) {

        SellerBalance balance = sellerBalanceService.adjustBalance(sellerId, request);

        return ResponseEntity.ok(ApiResponse.success(
                SellerBalanceResponse.fromEntity(balance),
                "Balance adjusted successfully"
        ));
    }

    // ==================== PUBLIC ENDPOINTS ====================

    @GetMapping("/{sellerId}/public")
    @Operation(summary = "Get public seller profile")
    public ResponseEntity<ApiResponse<PublicSellerResponse>> getPublicProfile(
            @PathVariable UUID sellerId) {

        PublicSellerResponse response = sellerService.getPublicProfile(sellerId);

        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Public profile retrieved"
        ));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get seller by slug")
    public ResponseEntity<ApiResponse<PublicSellerResponse>> getSellerBySlug(
            @PathVariable String slug) {

        PublicSellerResponse response = sellerService.getSellerBySlug(slug);

        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Seller profile retrieved"
        ));
    }
}