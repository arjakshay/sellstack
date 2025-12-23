package com.stack.sellstack.model.dto.response;

import com.stack.sellstack.model.entity.Seller;
import com.stack.sellstack.model.enums.SellerStatus;
import com.stack.sellstack.model.enums.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerResponse {
    private UUID id;
    private String email;
    private String phone;
    private String displayName;
    private String fullName;
    private String profilePicUrl;
    private String bio;
    private SellerStatus status;
    private VerificationStatus verificationStatus;
    private BigDecimal totalEarnings;
    private BigDecimal availableBalance;
    private Integer totalSales;
    private Integer totalProducts;
    private BigDecimal ratingAvg;
    private Integer ratingCount;
    private Instant lastLoginAt;
    private Instant createdAt;

    public static SellerResponse fromEntity(Seller seller) {
        if (seller == null) return null;

        return SellerResponse.builder()
                .id(seller.getId())
                .email(seller.getEmail())
                .phone(seller.getPhone())
                .displayName(seller.getDisplayName())
                .fullName(seller.getFullName())
                .profilePicUrl(seller.getProfilePicUrl())
                .bio(seller.getBio())
                .status(seller.getStatus())
                .verificationStatus(seller.getVerificationStatus())
                .totalEarnings(seller.getTotalEarnings())
                .availableBalance(seller.getAvailableBalance())
                .totalSales(seller.getTotalSales())
                .totalProducts(seller.getTotalProducts())
                .ratingAvg(seller.getRatingAvg())
                .ratingCount(seller.getRatingCount())
                .lastLoginAt(seller.getLastLoginAt())
                .createdAt(seller.getCreatedAt())
                .build();
    }
}
