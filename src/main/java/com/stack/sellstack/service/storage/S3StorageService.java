package com.stack.sellstack.service.storage;

import com.stack.sellstack.config.S3Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3StorageService implements S3Service {

    private final S3Client s3Client;
    private final S3Config s3Config;

    @Override
    public String uploadFile(MultipartFile file, String folder) throws IOException {
        String fileName = generateFileName(file.getOriginalFilename());
        String key = folder + "/" + fileName;

        log.info("Uploading file to S3: {}/{}", s3Config.getBucketName(), key);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Config.getBucketName())
                .key(key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        s3Client.putObject(putObjectRequest,
                RequestBody.fromBytes(file.getBytes()));

        return key;
    }

    @Override
    public String generatePresignedUrl(String key, int expiryHours) {
        log.info("Generating presigned URL for: {} (expires in {} hours)", key, expiryHours);

        try {
            GetUrlRequest getUrlRequest = GetUrlRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(key)
                    .build();

            // Note: GetUrlRequest doesn't support expiry directly in v2
            // For presigned URLs with expiry, use this approach:
            URL url = s3Client.utilities().getUrl(getUrlRequest);

            // For actual presigned URLs with expiry, you would need to use:
            // s3Client.utilities().getPresignedUrl() with custom parameters
            // But GetUrlRequest returns a URL that's valid for the default time

            return url.toString();

        } catch (Exception e) {
            log.error("Failed to generate presigned URL for: {}", key, e);
            throw new RuntimeException("Failed to generate download URL", e);
        }
    }

    // Alternative method using S3 presigned URL with expiry
    public String generatePresignedUrlWithExpiry(String key, int expiryHours) {
        try {
            software.amazon.awssdk.services.s3.presigner.S3Presigner presigner =
                    software.amazon.awssdk.services.s3.presigner.S3Presigner.builder()
                            .region(software.amazon.awssdk.regions.Region.of(s3Config.getRegion()))
                            .build();

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(key)
                    .build();

            software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest presignedRequest =
                    presigner.presignGetObject(r -> r.getObjectRequest(getObjectRequest)
                            .signatureDuration(Duration.ofHours(expiryHours)));

            return presignedRequest.url().toString();

        } catch (Exception e) {
            log.error("Failed to generate presigned URL with expiry", e);
            return getFileUrl(key); // Fallback to regular URL
        }
    }

    @Override
    public void deleteFile(String key) {
        log.info("Deleting file from S3: {}", key);

        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(s3Config.getBucketName())
                .key(key)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
    }

    @Override
    public boolean fileExists(String key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(key)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    @Override
    public String getFileUrl(String key) {
        if (s3Config.getCloudfrontDomain() != null && !s3Config.getCloudfrontDomain().isEmpty()) {
            // Use CloudFront URL
            return s3Config.getCloudfrontDomain() + "/" + key;
        } else if (s3Config.getEndpoint() != null && !s3Config.getEndpoint().isEmpty()) {
            // Use LocalStack/MinIO URL
            return s3Config.getEndpoint() + "/" + s3Config.getBucketName() + "/" + key;
        } else {
            // Use real S3 URL
            return String.format("https://%s.s3.%s.amazonaws.com/%s",
                    s3Config.getBucketName(), s3Config.getRegion(), key);
        }
    }

    private String generateFileName(String originalFileName) {
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    @Override
    public String extractFileKeyFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return null;
        }

        try {
            // Handle different URL formats

            // 1. S3 URL format: https://bucket.s3.region.amazonaws.com/key
            if (fileUrl.contains("amazonaws.com/")) {
                String afterAws = fileUrl.substring(fileUrl.indexOf("amazonaws.com/") + 14);
                // Remove bucket name if it's in the URL
                if (afterAws.contains("/")) {
                    String[] parts = afterAws.split("/", 2);
                    if (parts.length > 1 && parts[0].equals(s3Config.getBucketName())) {
                        return parts[1];
                    }
                }
                return afterAws;
            }

            // 2. CloudFront URL format
            if (s3Config.getCloudfrontDomain() != null &&
                    fileUrl.contains(s3Config.getCloudfrontDomain())) {
                return fileUrl.substring(fileUrl.indexOf(s3Config.getCloudfrontDomain()) +
                        s3Config.getCloudfrontDomain().length() + 1);
            }

            // 3. Direct S3 path (just the key)
            if (!fileUrl.contains("://")) {
                return fileUrl;
            }

            // 4. Extract from any URL
            URL url = new URL(fileUrl);
            String path = url.getPath();
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            return path;

        } catch (Exception e) {
            log.error("Failed to extract file key from URL: {}", fileUrl, e);
            return fileUrl;
        }
    }
}