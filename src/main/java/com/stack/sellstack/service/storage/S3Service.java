package com.stack.sellstack.service.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;

public interface S3Service {

    String uploadFile(MultipartFile file, String folder) throws IOException;

    String generatePresignedUrl(String key, int expiryHours);

    void deleteFile(String key);

    boolean fileExists(String key);

    String getFileUrl(String key);
    String extractFileKeyFromUrl(String fileUrl);
}