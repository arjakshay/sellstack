package com.stack.sellstack.service.storage;

import com.stack.sellstack.config.S3Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
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
public class S3StorageService {

    private final S3Client s3Client;
    private final S3Config s3Config;

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

    public URL generatePresignedUrl(String key) {
        log.info("Generating presigned URL for: {}", key);

        GetUrlRequest getUrlRequest = GetUrlRequest.builder()
                .bucket(s3Config.getBucketName())
                .key(key)
                .build();

        return s3Client.utilities().getUrl(getUrlRequest);
    }

    public void deleteFile(String key) {
        log.info("Deleting file from S3: {}", key);

        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(s3Config.getBucketName())
                .key(key)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
    }

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
}