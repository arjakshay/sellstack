package com.stack.sellstack.model.dto.request;

import com.stack.sellstack.model.entity.FileMetadata;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Data
public class FileUploadRequest {

    @Data
    public static class GeneratePresignedUrlRequest {
        @NotBlank(message = "Filename is required")
        private String filename;

        @NotBlank(message = "Content type is required")
        private String contentType;

        @NotNull(message = "File type is required")
        private FileMetadata.FileType fileType;
    }

    @Data
    public static class ConfirmUploadRequest {
        @NotNull(message = "File ID is required")
        private UUID fileId;

        @NotNull(message = "File size is required")
        private Long fileSize;

        @NotBlank(message = "MD5 hash is required")
        private String md5Hash;
    }

    @Data
    public static class DirectUploadRequest {
        @NotNull(message = "File is required")
        private MultipartFile file;

        @NotNull(message = "File type is required")
        private FileMetadata.FileType fileType;
    }
}



