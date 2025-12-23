package com.stack.sellstack.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    // Main fields at the top level
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private SellerResponse seller;

    // Inner classes for specific responses
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RefreshTokenResponse {
        private String accessToken;
        private String tokenType;
        private Long expiresIn;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OTPResponse {
        private String message;
        private String maskedPhone;
        private Integer validityMinutes;
        private Instant expiresAt;
    }
}