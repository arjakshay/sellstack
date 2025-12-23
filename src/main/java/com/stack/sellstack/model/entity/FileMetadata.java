package com.stack.sellstack.model.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stack.sellstack.model.entity.base.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "file_metadata",
        indexes = {
                @Index(name = "idx_file_metadata_seller_id", columnList = "seller_id"),
                @Index(name = "idx_file_metadata_status", columnList = "status"),
                @Index(name = "idx_file_metadata_created_at", columnList = "created_at DESC")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadata extends BaseAuditEntity {

    public enum FileStatus {
        UPLOADING,
        UPLOADED,
        PROCESSING,
        SCANNING,
        OPTIMIZING,
        READY,
        INFECTED,
        CORRUPTED,
        DELETED
    }

    public enum FileType {
        PRODUCT_FILE,
        THUMBNAIL,
        PREVIEW,
        GALLERY_IMAGE,
        PROFILE_PICTURE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, length = 255)
    private String storedFilename;

    @Column(name = "file_key", nullable = false, length = 1000)
    private String fileKey; // S3 key

    @Column(name = "file_url", length = 1000)
    private String fileUrl;

    @Column(name = "cdn_url", length = 1000)
    private String cdnUrl;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "file_extension", length = 10)
    private String fileExtension;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 50)
    private FileType fileType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private FileStatus status;

    @Column(name = "md5_hash", length = 32)
    private String md5Hash;

    @Column(name = "sha256_hash", length = 64)
    private String sha256Hash;

    @Column(name = "virus_scan_result")
    private Boolean virusScanResult;

    @Column(name = "virus_scan_timestamp")
    private Instant virusScanTimestamp;

    @Column(name = "optimization_applied")
    private Boolean optimizationApplied;

    @Column(name = "watermark_applied")
    private Boolean watermarkApplied;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "duration") // For video/audio files
    private Long duration;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "upload_completed_at")
    private Instant uploadCompletedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    // Transient field for metadata (not stored in DB)
    @Transient
    private Map<String, String> metadata = new HashMap<>();

    @PrePersist
    private void onPrePersist() {
        // Set default values
        if (status == null) {
            status = FileStatus.UPLOADING;
        }
        if (fileSize == null) {
            fileSize = 0L;
        }
        // Serialize metadata to JSON
        serializeMetadataToJson();
    }

    @PostLoad
    private void onPostLoad() {
        // Deserialize metadata from JSON
        deserializeMetadataFromJson();
    }

    @PreUpdate
    private void onPreUpdate() {
        // Update metadata before saving
        serializeMetadataToJson();
    }

    // Helper method to serialize metadata to JSON
    private void serializeMetadataToJson() {
        if (metadata != null && !metadata.isEmpty()) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                this.metadataJson = objectMapper.writeValueAsString(metadata);
            } catch (JsonProcessingException e) {
                this.metadataJson = "{}";
            }
        } else {
            this.metadataJson = "{}";
        }
    }

    // Helper method to deserialize metadata from JSON
    private void deserializeMetadataFromJson() {
        if (metadataJson != null && !metadataJson.isEmpty()) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                this.metadata = objectMapper.readValue(metadataJson,
                        new TypeReference<Map<String, String>>() {});
            } catch (IOException e) {
                this.metadata = new HashMap<>();
            }
        } else {
            this.metadata = new HashMap<>();
        }
    }

    // Convenience methods for metadata
    public Map<String, String> getMetadata() {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
        serializeMetadataToJson();
    }

    public void addMetadata(String key, String value) {
        getMetadata().put(key, value);
        serializeMetadataToJson();
    }

    public String getMetadata(String key) {
        return getMetadata().get(key);
    }

    public void removeMetadata(String key) {
        getMetadata().remove(key);
        serializeMetadataToJson();
    }
}