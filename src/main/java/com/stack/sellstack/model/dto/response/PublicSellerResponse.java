package com.stack.sellstack.model.dto.response;

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
public class PublicSellerResponse {
    private UUID id;
    private String displayName;
    private String profilePicUrl;
    private String bio;
    private Integer totalProducts;
    private BigDecimal ratingAvg;
    private Integer ratingCount;
    private Instant createdAt;
}