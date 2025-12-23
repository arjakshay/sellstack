package com.stack.sellstack.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "aws.s3")
public class S3Config {

    private String accessKey;
    private String secretKey;
    private String region;
    private String bucketName;
    private String endpoint; // For LocalStack testing
    private boolean pathStyleAccessEnabled = false;

    // Security settings
    private String kmsKeyId; // For SSE-KMS
    private boolean encryptionEnabled = true;
    private String encryptionAlgorithm = "AES256";

    // URL settings
    private int presignedUrlExpiryMinutes = 15;
    private int downloadUrlExpiryHours = 24;

    // Limits
    private long maxFileSizeBytes = 500 * 1024 * 1024L; // 500MB
    private long maxImageSizeBytes = 10 * 1024 * 1024L; // 10MB
    private long maxThumbnailSizeBytes = 2 * 1024 * 1024L; // 2MB

    // CloudFront
    private String cloudfrontDomain;
    private String cloudfrontKeyPairId;
    private String cloudfrontPrivateKey;
}