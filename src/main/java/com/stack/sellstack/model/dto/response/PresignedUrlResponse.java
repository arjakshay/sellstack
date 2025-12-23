package com.stack.sellstack.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUrlResponse {
    private String uploadUrl;
    private String downloadUrl;
    private UUID fileId;
    private String fileKey;
    private Instant expiresAt;
    private String bucketName;
    private String region;

    public static PresignedUrlResponse from(String uploadUrl, String downloadUrl, UUID fileId,
                                            String fileKey, Instant expiresAt, String bucketName, String region) {
        return PresignedUrlResponse.builder()
                .uploadUrl(uploadUrl)
                .downloadUrl(downloadUrl)
                .fileId(fileId)
                .fileKey(fileKey)
                .expiresAt(expiresAt)
                .bucketName(bucketName)
                .region(region)
                .build();
    }
}