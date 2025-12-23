package com.stack.sellstack.repository;

import com.stack.sellstack.model.entity.FileMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, UUID> {

    /**
     * Find files by seller ID
     */
    Page<FileMetadata> findBySellerId(UUID sellerId, Pageable pageable);

    /**
     * Find files by product ID
     */
    List<FileMetadata> findByProductId(UUID productId);

    /**
     * Find files by seller ID and status
     */
    Page<FileMetadata> findBySellerIdAndStatus(UUID sellerId, FileMetadata.FileStatus status, Pageable pageable);

    /**
     * Find file by S3 key
     */
    Optional<FileMetadata> findByFileKey(String fileKey);

    /**
     * Find files by type
     */
    Page<FileMetadata> findByFileType(FileMetadata.FileType fileType, Pageable pageable);

    /**
     * Find expired files
     */
    @Query("SELECT f FROM FileMetadata f WHERE f.expiresAt < :now AND f.status != 'DELETED'")
    List<FileMetadata> findExpiredFiles(@Param("now") Instant now);

    /**
     * Update file status
     */
    @Modifying
    @Query("UPDATE FileMetadata f SET f.status = :status WHERE f.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") FileMetadata.FileStatus status);

    /**
     * Mark file as processed
     */
    @Modifying
    @Query("UPDATE FileMetadata f SET f.status = 'READY', f.processedAt = :processedAt WHERE f.id = :id")
    void markAsProcessed(@Param("id") UUID id, @Param("processedAt") Instant processedAt);

    /**
     * Mark file as infected
     */
    @Modifying
    @Query("UPDATE FileMetadata f SET f.status = 'INFECTED', f.virusScanResult = false, f.virusScanTimestamp = :timestamp WHERE f.id = :id")
    void markAsInfected(@Param("id") UUID id, @Param("timestamp") Instant timestamp);

    /**
     * Clean up old file records
     */
    @Modifying
    @Query("DELETE FROM FileMetadata f WHERE f.isDeleted = true AND f.updatedAt < :cutoff")
    int deleteOldDeletedFiles(@Param("cutoff") Instant cutoff);

    /**
     * Count files by seller
     */
    Long countBySellerId(UUID sellerId);

    /**
     * Get total storage used by seller
     */
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM FileMetadata f WHERE f.sellerId = :sellerId AND f.isDeleted = false")
    Long getTotalStorageUsed(@Param("sellerId") UUID sellerId);
}