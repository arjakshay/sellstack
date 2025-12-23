package com.stack.sellstack.config;

import com.stack.sellstack.model.entity.FileMetadata;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "file.validation")
public class FileValidationConfig {

    // Allowed file types with MIME types
    private List<String> allowedMimeTypes = List.of(
            // Documents
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "application/rtf",

            // Archives
            "application/zip",
            "application/x-zip-compressed",
            "application/x-rar-compressed",

            // Audio
            "audio/mpeg",
            "audio/mp3",
            "audio/wav",
            "audio/ogg",
            "audio/x-m4a",

            // Video
            "video/mp4",
            "video/mpeg",
            "video/quicktime",
            "video/x-msvideo",
            "video/x-matroska",

            // Images (for thumbnails/previews)
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/svg+xml"
    );

    // Allowed file extensions
    private List<String> allowedExtensions = List.of(
            "pdf", "doc", "docx", "txt", "rtf",
            "zip", "rar", "7z",
            "mp3", "wav", "ogg", "m4a", "flac",
            "mp4", "avi", "mov", "mkv", "wmv",
            "jpg", "jpeg", "png", "gif", "webp", "svg"
    );

    // Dangerous file extensions to block
    private List<String> blockedExtensions = List.of(
            "exe", "bat", "cmd", "sh", "php", "asp", "aspx",
            "js", "html", "htm", "jar", "war", "ear",
            "dll", "sys", "vbs", "ps1", "py", "rb"
    );

    // Size limits - ADD THESE FIELDS
    private long maxFileSizeBytes = 500 * 1024 * 1024L; // 500MB
    private long maxImageSizeBytes = 10 * 1024 * 1024L; // 10MB
    private long maxThumbnailSizeBytes = 2 * 1024 * 1024L; // 2MB
    private long maxPreviewSizeBytes = 5 * 1024 * 1024L; // 5MB
    private long maxProfilePictureSizeBytes = 5 * 1024 * 1024L; // 5MB

    // Virus scanning
    private boolean enableVirusScanning = true;
    private String clamAvHost = "localhost";
    private int clamAvPort = 3310;

    // Image optimization
    private boolean enableImageOptimization = true;
    private int imageQuality = 85;
    private int thumbnailWidth = 400;
    private int thumbnailHeight = 400;
    private int previewWidth = 800;
    private int previewHeight = 600;

    // Watermark settings for digital products
    private boolean enableWatermarking = true;
    private String watermarkText = "SellStack";
    private String watermarkFont = "Arial";
    private int watermarkFontSize = 24;
    private String watermarkColor = "#FFFFFF";
    private int watermarkOpacity = 30;

    // Compression settings
    private boolean enableCompression = true;
    private int compressionQuality = 80;
    private boolean stripMetadata = true;

    // Security settings
    private boolean validateFileSignature = true;
    private boolean checkMagicNumbers = true;
    private boolean enforceFilenameSanitization = true;

    // Getter methods for different file types
    public long getMaxSizeForFileType(FileMetadata.FileType fileType) {
        return switch (fileType) {
            case PRODUCT_FILE -> maxFileSizeBytes;
            case THUMBNAIL -> maxThumbnailSizeBytes;
            case PREVIEW -> maxPreviewSizeBytes;
            case GALLERY_IMAGE -> maxImageSizeBytes;
            case PROFILE_PICTURE -> maxProfilePictureSizeBytes;
        };
    }

    public String getSizeLimitMessage(FileMetadata.FileType fileType) {
        long maxSize = getMaxSizeForFileType(fileType);
        String fileTypeName = fileType.toString().toLowerCase().replace("_", " ");

        if (maxSize >= 1024 * 1024 * 1024) { // GB
            return String.format("%s size must be less than %.1f GB",
                    fileTypeName, maxSize / (1024.0 * 1024.0 * 1024.0));
        } else if (maxSize >= 1024 * 1024) { // MB
            return String.format("%s size must be less than %.1f MB",
                    fileTypeName, maxSize / (1024.0 * 1024.0));
        } else { // KB
            return String.format("%s size must be less than %.1f KB",
                    fileTypeName, maxSize / 1024.0);
        }
    }
}