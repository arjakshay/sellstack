package com.stack.sellstack.model.dto.response;


import com.stack.sellstack.model.entity.FileMetadata;
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
public class FileUploadResponse {
    private UUID fileId;
    private String filename;
    private String originalFilename;
    private String fileUrl;
    private String cdnUrl;
    private Long fileSize;
    private String mimeType;
    private String fileExtension;
    private FileMetadata.FileType fileType;
    private FileMetadata.FileStatus status;
    private Instant uploadCompletedAt;

    public static FileUploadResponse fromEntity(FileMetadata metadata) {
        if (metadata == null) return null;

        return FileUploadResponse.builder()
                .fileId(metadata.getId())
                .filename(metadata.getStoredFilename())
                .originalFilename(metadata.getOriginalFilename())
                .fileUrl(metadata.getFileUrl())
                .cdnUrl(metadata.getCdnUrl())
                .fileSize(metadata.getFileSize())
                .mimeType(metadata.getMimeType())
                .fileExtension(metadata.getFileExtension())
                .fileType(metadata.getFileType())
                .status(metadata.getStatus())
                .uploadCompletedAt(metadata.getUploadCompletedAt())
                .build();
    }
}