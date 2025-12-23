package com.stack.sellstack.service.storage;

import com.stack.sellstack.config.FileValidationConfig;
import com.stack.sellstack.config.S3Config;
import com.stack.sellstack.exception.BusinessException;
import com.stack.sellstack.exception.ValidationException;
import com.stack.sellstack.model.dto.response.PresignedUrlResponse;
import com.stack.sellstack.model.entity.FileMetadata;
import com.stack.sellstack.repository.FileMetadataRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileStorageService {

    private final S3Config s3Config;
    private final FileValidationConfig validationConfig;
    private final FileMetadataRepository fileMetadataRepository;
    private final FileValidator fileValidator;
    private final VirusScanner virusScanner;
    private final ImageOptimizer imageOptimizer;

    private S3Client s3Client;
    private S3Presigner s3Presigner;

    @PostConstruct
    public void init() {
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(
                s3Config.getAccessKey(),
                s3Config.getSecretKey()
        );

        S3ClientBuilder builder = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .region(Region.of(s3Config.getRegion()));

        if (s3Config.getEndpoint() != null && !s3Config.getEndpoint().isEmpty()) {
            builder.endpointOverride(URI.create(s3Config.getEndpoint()));
            builder.serviceConfiguration(S3Configuration.builder()
                    .pathStyleAccessEnabled(s3Config.isPathStyleAccessEnabled())
                    .build());
        }

        this.s3Client = builder.build();
        this.s3Presigner = S3Presigner.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .region(Region.of(s3Config.getRegion()))
                .build();
    }

    /**
     * Generate presigned URL for direct S3 upload (Frontend → S3)
     */
    @Transactional
    public PresignedUrlResponse generatePresignedUrl(UUID sellerId,
                                                     String originalFilename,
                                                     String contentType,
                                                     FileMetadata.FileType fileType) {

        // Validate file
        fileValidator.validateFilename(originalFilename);
        fileValidator.validateContentType(contentType);

        // Generate unique file key
        String fileKey = generateFileKey(sellerId, originalFilename, fileType);
        String storedFilename = generateStoredFilename(originalFilename);

        // Create file metadata record
        FileMetadata metadata = FileMetadata.builder()
                .sellerId(sellerId)
                .originalFilename(originalFilename)
                .storedFilename(storedFilename)
                .fileKey(fileKey)
                .fileSize(0L) // Will be updated after upload
                .mimeType(contentType)
                .fileExtension(FilenameUtils.getExtension(originalFilename).toLowerCase())
                .fileType(fileType)
                .status(FileMetadata.FileStatus.UPLOADING)
                .build();

        fileMetadataRepository.save(metadata);

        // Generate presigned URL
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Config.getBucketName())
                .key(fileKey)
                .contentType(contentType)
                .metadata(Map.of(
                        "seller-id", sellerId.toString(),
                        "file-id", metadata.getId().toString(),
                        "original-filename", originalFilename
                ))
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(s3Config.getPresignedUrlExpiryMinutes()))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        String uploadUrl = presignedRequest.url().toString();

        // Generate download URL (will be active after upload)
        String downloadUrl = generateSecureDownloadUrl(fileKey, s3Config.getDownloadUrlExpiryHours());

        return PresignedUrlResponse.builder()
                .uploadUrl(uploadUrl)
                .downloadUrl(downloadUrl)
                .fileId(metadata.getId())
                .fileKey(fileKey)
                .expiresAt(presignedRequest.expiration())
                .build();
    }

    /**
     * Confirm upload completion and trigger processing
     */
    @Transactional
    public void confirmUpload(UUID fileId, Long fileSize, String md5Hash) {
        FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException("File not found"));

        // Verify file exists in S3
        try {
            HeadObjectResponse headResponse = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(metadata.getFileKey())
                    .build());

            metadata.setFileSize(headResponse.contentLength());
            metadata.setMd5Hash(md5Hash);
            metadata.setStatus(FileMetadata.FileStatus.UPLOADED);
            metadata.setUploadCompletedAt(Instant.now());

            // Generate permanent URLs
            metadata.setFileUrl(generateS3Url(metadata.getFileKey()));
            metadata.setCdnUrl(generateCdnUrl(metadata.getFileKey()));

            fileMetadataRepository.save(metadata);

            log.info("File upload confirmed: {}", metadata.getFileKey());

            // Trigger async processing
            processFileAsync(metadata);

        } catch (NoSuchKeyException e) {
            throw new BusinessException("File not found in storage");
        }
    }

    /**
     * Direct upload (for small files)
     */
    @Transactional
    public FileMetadata uploadFile(UUID sellerId, MultipartFile file, FileMetadata.FileType fileType) {
        try {
            // Validate file
            fileValidator.validateFile(file, fileType);

            // Generate metadata
            String originalFilename = file.getOriginalFilename();
            String fileKey = generateFileKey(sellerId, originalFilename, fileType);
            String storedFilename = generateStoredFilename(originalFilename);

            // Calculate hashes
            String md5Hash = calculateMd5(file.getBytes());
            String sha256Hash = calculateSha256(file.getBytes());

            // Create metadata record
            FileMetadata metadata = FileMetadata.builder()
                    .sellerId(sellerId)
                    .originalFilename(originalFilename)
                    .storedFilename(storedFilename)
                    .fileKey(fileKey)
                    .fileSize(file.getSize())
                    .mimeType(file.getContentType())
                    .fileExtension(FilenameUtils.getExtension(originalFilename).toLowerCase())
                    .fileType(fileType)
                    .status(FileMetadata.FileStatus.UPLOADED)
                    .md5Hash(md5Hash)
                    .sha256Hash(sha256Hash)
                    .uploadCompletedAt(Instant.now())
                    .build();

            // Upload to S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(fileKey)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .metadata(Map.of(
                            "seller-id", sellerId.toString(),
                            "file-id", metadata.getId().toString(),
                            "original-filename", originalFilename,
                            "md5-hash", md5Hash
                    ))
                    .build();

            s3Client.putObject(putObjectRequest,
                    RequestBody.fromByteBuffer(ByteBuffer.wrap(file.getBytes())));

            // Generate URLs
            metadata.setFileUrl(generateS3Url(fileKey));
            metadata.setCdnUrl(generateCdnUrl(fileKey));

            fileMetadataRepository.save(metadata);

            log.info("File uploaded directly: {}", fileKey);

            // Trigger async processing
            processFileAsync(metadata);

            return metadata;

        } catch (IOException e) {
            log.error("File upload failed", e);
            throw new BusinessException("File upload failed");
        }
    }

    /**
     * Generate secure download URL (signed)
     */
    public String generateDownloadUrl(UUID fileId, int expiryHours) {
        FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException("File not found"));

        if (!FileMetadata.FileStatus.READY.equals(metadata.getStatus())) {
            throw new BusinessException("File is not ready for download");
        }

        return generateSecureDownloadUrl(metadata.getFileKey(), expiryHours);
    }

    /**
     * Delete file (soft delete)
     */
    @Transactional
    public void deleteFile(UUID fileId) {
        FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException("File not found"));

        // Mark as deleted in database
        metadata.setStatus(FileMetadata.FileStatus.DELETED);
        metadata.setIsDeleted(true);
        fileMetadataRepository.save(metadata);

        log.info("File marked as deleted: {}", metadata.getFileKey());

        // Note: We don't delete from S3 immediately for recovery purposes
        // Actual deletion can be done via lifecycle policy or manual cleanup
    }

    /**
     * Generate file key for S3
     */
    private String generateFileKey(UUID sellerId, String filename, FileMetadata.FileType fileType) {
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String random = UUID.randomUUID().toString().substring(0, 8);
        String extension = FilenameUtils.getExtension(filename).toLowerCase();

        String folder = switch (fileType) {
            case PRODUCT_FILE -> "products";
            case THUMBNAIL -> "thumbnails";
            case PREVIEW -> "previews";
            case GALLERY_IMAGE -> "gallery";
            case PROFILE_PICTURE -> "profiles";
        };

        return String.format("%s/%s/%s-%s.%s",
                folder,
                sellerId,
                timestamp,
                random,
                extension);
    }

    private String generateStoredFilename(String originalFilename) {
        String baseName = FilenameUtils.getBaseName(originalFilename);
        String extension = FilenameUtils.getExtension(originalFilename);
        String timestamp = String.valueOf(Instant.now().toEpochMilli());

        return String.format("%s-%s.%s",
                baseName.substring(0, Math.min(baseName.length(), 50)),
                timestamp,
                extension.toLowerCase());
    }

    /**
     * Generate secure signed URL for download
     */
    private String generateSecureDownloadUrl(String fileKey, int expiryHours) {
        if (s3Config.getCloudfrontDomain() != null &&
                s3Config.getCloudfrontKeyPairId() != null &&
                s3Config.getCloudfrontPrivateKey() != null) {
            // Use CloudFront signed URL
            return generateCloudFrontSignedUrl(fileKey, expiryHours);
        } else {
            // Use S3 presigned URL
            return generateS3PresignedUrl(fileKey, expiryHours);
        }
    }

    private String generateS3PresignedUrl(String fileKey, int expiryHours) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Config.getBucketName())
                .key(fileKey)
                .build();

        return s3Presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofHours(expiryHours))
                        .getObjectRequest(getObjectRequest)
                        .build()
        ).url().toString();
    }

    private String generateCloudFrontSignedUrl(String fileKey, int expiryHours) {
        // Implementation for CloudFront signed URLs
        // This requires additional setup with CloudFront key pairs
        try {
            String resourceUrl = s3Config.getCloudfrontDomain() + "/" + fileKey;
            Date expires = new Date(System.currentTimeMillis() + (expiryHours * 60 * 60 * 1000));

            // CloudFront signer implementation
            // Note: This is simplified. Actual implementation requires proper key loading
            return resourceUrl + "?Expires=" + (expires.getTime() / 1000) +
                    "&Signature=[SIGNATURE]&Key-Pair-Id=" + s3Config.getCloudfrontKeyPairId();
        } catch (Exception e) {
            log.warn("Failed to generate CloudFront URL, falling back to S3", e);
            return generateS3PresignedUrl(fileKey, expiryHours);
        }
    }

    private String generateS3Url(String fileKey) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                s3Config.getBucketName(),
                s3Config.getRegion(),
                fileKey);
    }

    private String generateCdnUrl(String fileKey) {
        if (s3Config.getCloudfrontDomain() != null) {
            return s3Config.getCloudfrontDomain() + "/" + fileKey;
        }
        return generateS3Url(fileKey);
    }

    /**
     * Async file processing pipeline
     */
    @Async
    @Transactional
    public void processFileAsync(FileMetadata metadata) {
        try {
            metadata.setStatus(FileMetadata.FileStatus.PROCESSING);
            fileMetadataRepository.save(metadata);

            // 1. Virus scanning
            if (validationConfig.isEnableVirusScanning()) {
                metadata.setStatus(FileMetadata.FileStatus.SCANNING);
                fileMetadataRepository.save(metadata);

                boolean isClean = virusScanner.scanFile(metadata.getFileKey());
                metadata.setVirusScanResult(isClean);
                metadata.setVirusScanTimestamp(Instant.now());

                if (!isClean) {
                    metadata.setStatus(FileMetadata.FileStatus.INFECTED);
                    fileMetadataRepository.save(metadata);
                    log.warn("Virus detected in file: {}", metadata.getFileKey());
                    return;
                }
            }

            // 2. Image optimization (if image)
            if (isImageFile(metadata.getMimeType()) && validationConfig.isEnableImageOptimization()) {
                metadata.setStatus(FileMetadata.FileStatus.OPTIMIZING);
                fileMetadataRepository.save(metadata);

                boolean optimized = imageOptimizer.optimizeImage(metadata);
                metadata.setOptimizationApplied(optimized);
            }

            // 3. Extract metadata (dimensions, duration, etc.)
            extractFileMetadata(metadata);

            // 4. Apply watermark if needed
            if (metadata.getFileType() == FileMetadata.FileType.PRODUCT_FILE &&
                    validationConfig.isEnableWatermarking()) {
                applyWatermark(metadata);
            }

            metadata.setStatus(FileMetadata.FileStatus.READY);
            metadata.setProcessedAt(Instant.now());
            fileMetadataRepository.save(metadata);

            log.info("File processing completed: {}", metadata.getFileKey());

        } catch (Exception e) {
            log.error("File processing failed: {}", metadata.getFileKey(), e);
            metadata.setStatus(FileMetadata.FileStatus.CORRUPTED);
            fileMetadataRepository.save(metadata);
        }
    }

    // Helper methods for hash calculation
    private String calculateMd5(byte[] data) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data);
            return bytesToHex(digest);
        } catch (Exception e) {
            throw new IOException("Failed to calculate MD5", e);
        }
    }

    private String calculateSha256(byte[] data) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            return bytesToHex(digest);
        } catch (Exception e) {
            throw new IOException("Failed to calculate SHA256", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private boolean isImageFile(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }

    private void extractFileMetadata(FileMetadata metadata) {
        // Extract dimensions, duration, etc. based on file type
        // Implementation depends on file type
    }

    private void applyWatermark(FileMetadata metadata) {
        // Apply watermark for digital products
        // Implementation for images/PDFs
    }
}