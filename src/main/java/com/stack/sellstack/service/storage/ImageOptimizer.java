package com.stack.sellstack.service.storage;

import com.stack.sellstack.config.FileValidationConfig;
import com.stack.sellstack.config.S3Config;
import com.stack.sellstack.model.entity.FileMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageOptimizer {

    private final S3Client s3Client;
    private final S3Config s3Config;
    private final FileValidationConfig config;

    public boolean optimizeImage(FileMetadata metadata) {
        if (!isImageFile(metadata.getMimeType())) {
            return false;
        }

        try {
            // Download image from S3
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(metadata.getFileKey())
                    .build();

            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(getRequest);
            BufferedImage originalImage = ImageIO.read(response);
            response.close();

            if (originalImage == null) {
                log.warn("Failed to read image: {}", metadata.getFileKey());
                return false;
            }

            // Extract dimensions
            metadata.setWidth(originalImage.getWidth());
            metadata.setHeight(originalImage.getHeight());

            // Optimize based on file type
            byte[] optimizedImage;
            String optimizedMimeType = metadata.getMimeType();

            if (metadata.getMimeType().equals("image/jpeg") ||
                    metadata.getMimeType().equals("image/jpg")) {
                optimizedImage = optimizeJpeg(originalImage);
            } else if (metadata.getMimeType().equals("image/png")) {
                optimizedImage = optimizePng(originalImage);
            } else if (metadata.getMimeType().equals("image/webp")) {
                optimizedImage = optimizeWebp(originalImage);
            } else {
                // For other formats, just compress
                optimizedImage = compressImage(originalImage, metadata.getMimeType());
            }

            // Upload optimized image back to S3
            String optimizedKey = metadata.getFileKey() + ".optimized";

            // Get metadata from FileMetadata object
            Map<String, String> metadataMap = metadata.getMetadata();

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(optimizedKey)
                    .contentType(optimizedMimeType)
                    .contentLength((long) optimizedImage.length)
                    .metadata(metadataMap) // Use the metadata map
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(optimizedImage));

            // Update metadata
            metadata.setFileKey(optimizedKey);
            metadata.setFileSize((long) optimizedImage.length);
            metadata.setMimeType(optimizedMimeType);

            log.info("Image optimized: {} ({} → {} bytes)",
                    metadata.getOriginalFilename(),
                    response.response().contentLength(),
                    optimizedImage.length);

            return true;

        } catch (Exception e) {
            log.error("Image optimization failed: {}", metadata.getFileKey(), e);
            return false;
        }
    }

    private byte[] optimizeJpeg(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer available");
        }

        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();

        // Set compression quality
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(config.getImageQuality() / 100.0f);

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }

        return baos.toByteArray();
    }

    private byte[] optimizePng(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // PNG optimization is more complex, we can use libraries like PNGJ
        // For simplicity, we'll just write with default settings
        ImageIO.write(image, "png", baos);

        return baos.toByteArray();
    }

    private byte[] optimizeWebp(BufferedImage image) throws IOException {
        // WebP optimization requires additional libraries
        // For now, return original
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "webp", baos);
        return baos.toByteArray();
    }

    private byte[] compressImage(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format.split("/")[1], baos);
        return baos.toByteArray();
    }

    private boolean isImageFile(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }

    /**
     * Generate thumbnail for image
     */
    public byte[] generateThumbnail(BufferedImage originalImage) {
        int targetWidth = config.getThumbnailWidth();
        int targetHeight = config.getThumbnailHeight();

        // Calculate scaling
        double scale = Math.min(
                (double) targetWidth / originalImage.getWidth(),
                (double) targetHeight / originalImage.getHeight()
        );

        int scaledWidth = (int) (originalImage.getWidth() * scale);
        int scaledHeight = (int) (originalImage.getHeight() * scale);

        // Create thumbnail
        BufferedImage thumbnail = new BufferedImage(scaledWidth, scaledHeight,
                BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = thumbnail.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();

        // Convert to bytes
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(thumbnail, "jpg", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate thumbnail", e);
            return null;
        }
    }
}