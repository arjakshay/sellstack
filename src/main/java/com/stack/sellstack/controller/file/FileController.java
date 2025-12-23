package com.stack.sellstack.controller.file;


import com.stack.sellstack.model.dto.response.ApiResponse;
import com.stack.sellstack.model.dto.response.PresignedUrlResponse;
import com.stack.sellstack.model.entity.FileMetadata;
import com.stack.sellstack.security.CurrentUser;
import com.stack.sellstack.service.storage.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File Management", description = "File upload and management APIs")
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/presigned-url")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Generate presigned URL for direct S3 upload",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> generatePresignedUrl(
            @CurrentUser UUID sellerId,
            @Valid @RequestBody FileUploadRequest request) {

        log.info("Generating presigned URL for seller: {}, file: {}",
                sellerId, request.getFilename());

        PresignedUrlResponse response = fileStorageService.generatePresignedUrl(
                sellerId,
                request.getFilename(),
                request.getContentType(),
                request.getFileType()
        );

        return ResponseEntity.ok(ApiResponse.success(response, "Presigned URL generated"));
    }

    @PostMapping("/confirm-upload")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Confirm file upload completion",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> confirmUpload(
            @CurrentUser UUID sellerId,
            @Valid @RequestBody ConfirmUploadRequest request) {

        log.info("Confirming upload for file: {}", request.getFileId());

        // Verify file belongs to seller
        // (Implementation in service layer)

        fileStorageService.confirmUpload(
                request.getFileId(),
                request.getFileSize(),
                request.getMd5Hash()
        );

        return ResponseEntity.ok(ApiResponse.success(null, "Upload confirmed"));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Direct file upload (for small files)",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @CurrentUser UUID sellerId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileType") FileMetadata.FileType fileType) {

        log.info("Direct file upload by seller: {}, size: {}",
                sellerId, file.getSize());

        FileMetadata metadata = fileStorageService.uploadFile(sellerId, file, fileType);

        FileUploadResponse response = FileUploadResponse.builder()
                .fileId(metadata.getId())
                .filename(metadata.getOriginalFilename())
                .fileUrl(metadata.getFileUrl())
                .cdnUrl(metadata.getCdnUrl())
                .fileSize(metadata.getFileSize())
                .mimeType(metadata.getMimeType())
                .status(metadata.getStatus())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response, "File uploaded successfully"));
    }

    @GetMapping("/{fileId}/download-url")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Generate download URL for file",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<DownloadUrlResponse>> generateDownloadUrl(
            @CurrentUser UUID sellerId,
            @PathVariable UUID fileId,
            @RequestParam(defaultValue = "24") int expiryHours) {

        log.info("Generating download URL for file: {}, seller: {}", fileId, sellerId);

        String downloadUrl = fileStorageService.generateDownloadUrl(fileId, expiryHours);

        DownloadUrlResponse response = DownloadUrlResponse.builder()
                .downloadUrl(downloadUrl)
                .expiryHours(expiryHours)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response, "Download URL generated"));
    }

    @DeleteMapping("/{fileId}")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Delete file",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @CurrentUser UUID sellerId,
            @PathVariable UUID fileId) {

        log.info("Deleting file: {}, seller: {}", fileId, sellerId);

        fileStorageService.deleteFile(fileId);

        return ResponseEntity.ok(ApiResponse.success(null, "File deleted"));
    }

    // DTOs for this controller
    @lombok.Data
    public static class FileUploadRequest {
        private String filename;
        private String contentType;
        private FileMetadata.FileType fileType;
    }

    @lombok.Data
    public static class ConfirmUploadRequest {
        private UUID fileId;
        private Long fileSize;
        private String md5Hash;
    }

    @lombok.Data
    @lombok.Builder
    public static class FileUploadResponse {
        private UUID fileId;
        private String filename;
        private String fileUrl;
        private String cdnUrl;
        private Long fileSize;
        private String mimeType;
        private FileMetadata.FileStatus status;
    }

    @lombok.Data
    @lombok.Builder
    public static class DownloadUrlResponse {
        private String downloadUrl;
        private int expiryHours;
    }
}