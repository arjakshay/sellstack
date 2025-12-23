package com.stack.sellstack.service.storage;

import com.stack.sellstack.config.FileValidationConfig;
import com.stack.sellstack.exception.ValidationException;
import com.stack.sellstack.model.entity.FileMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileValidator {

    private final FileValidationConfig config;

    public void validateFile(MultipartFile file, FileMetadata.FileType fileType) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("File cannot be empty");
        }

        validateFilename(file.getOriginalFilename());
        validateContentType(file.getContentType());
        validateFileSize(file, fileType);
        validateExtension(file.getOriginalFilename());
    }

    public void validateFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new ValidationException("Filename cannot be empty");
        }

        // Check for path traversal attempts
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new ValidationException("Invalid filename");
        }

        // Check filename length
        if (filename.length() > 255) {
            throw new ValidationException("Filename too long");
        }

        // Check for dangerous characters
        if (filename.matches(".*[<>:\"|?*].*")) {
            throw new ValidationException("Filename contains invalid characters");
        }
    }

    public void validateContentType(String contentType) {
        if (contentType == null || contentType.trim().isEmpty()) {
            throw new ValidationException("Content type cannot be empty");
        }

        if (!config.getAllowedMimeTypes().contains(contentType.toLowerCase())) {
            throw new ValidationException("File type not allowed: " + contentType);
        }
    }

    public void validateFileSize(MultipartFile file, FileMetadata.FileType fileType) {
        long maxSize = switch (fileType) {
            case PRODUCT_FILE -> config.getMaxFileSizeBytes();
            case THUMBNAIL -> config.getMaxThumbnailSizeBytes();
            case PREVIEW, GALLERY_IMAGE -> config.getMaxImageSizeBytes();
            case PROFILE_PICTURE -> config.getMaxImageSizeBytes();
        };

        if (file.getSize() > maxSize) {
            throw new ValidationException(
                    String.format("File size exceeds limit. Max: %.2f MB",
                            maxSize / (1024.0 * 1024.0))
            );
        }
    }

    public void validateExtension(String filename) {
        String extension = FilenameUtils.getExtension(filename).toLowerCase();

        if (config.getBlockedExtensions().contains(extension)) {
            throw new ValidationException("File extension not allowed: " + extension);
        }

        if (!config.getAllowedExtensions().contains(extension)) {
            throw new ValidationException("File extension not supported: " + extension);
        }
    }

    public boolean isImageFile(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }

    public boolean isVideoFile(String mimeType) {
        return mimeType != null && mimeType.startsWith("video/");
    }

    public boolean isAudioFile(String mimeType) {
        return mimeType != null && mimeType.startsWith("audio/");
    }

    public boolean isDocumentFile(String mimeType) {
        return mimeType != null && (
                mimeType.startsWith("application/pdf") ||
                        mimeType.contains("word") ||
                        mimeType.contains("text") ||
                        mimeType.contains("rtf")
        );
    }

    public boolean isArchiveFile(String mimeType) {
        return mimeType != null && (
                mimeType.contains("zip") ||
                        mimeType.contains("rar") ||
                        mimeType.contains("compressed")
        );
    }
}
