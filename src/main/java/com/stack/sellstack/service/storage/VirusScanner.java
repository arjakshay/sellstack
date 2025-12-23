package com.stack.sellstack.service.storage;

import com.stack.sellstack.config.FileValidationConfig;
import com.stack.sellstack.config.S3Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.*;
import java.net.Socket;

@Service
@Slf4j
@RequiredArgsConstructor
@Profile("!test & !dev") // Only enable in production
public class VirusScanner {

    private final S3Client s3Client;
    private final FileValidationConfig config;
    private final S3Config s3Config;

    /**
     * Scan file using ClamAV antivirus
     */
    public boolean scanFile(String fileKey) {
        if (!config.isEnableVirusScanning()) {
            return true; // Skip scanning if disabled
        }

        try {
            // Download file from S3
            File tempFile = downloadFileToTemp(fileKey);

            try {
                // Connect to ClamAV daemon
                Socket socket = new Socket(config.getClamAvHost(), config.getClamAvPort());
                DataInputStream dis = new DataInputStream(socket.getInputStream());
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

                // Send INSTREAM command
                dos.write("zINSTREAM\0".getBytes());
                dos.flush();

                // Send file in chunks
                FileInputStream fis = new FileInputStream(tempFile);
                byte[] buffer = new byte[2048];
                int bytesRead;

                while ((bytesRead = fis.read(buffer)) != -1) {
                    dos.writeInt(bytesRead);
                    dos.write(buffer, 0, bytesRead);
                }

                // Send zero-length chunk to indicate end
                dos.writeInt(0);
                dos.flush();
                fis.close();

                // Read response
                byte[] response = new byte[2048];
                int responseLength = dis.read(response);
                String result = new String(response, 0, responseLength).trim();

                socket.close();

                // Parse result
                boolean isClean = result.endsWith("OK");

                log.info("Virus scan result for {}: {} - {}",
                        fileKey, isClean ? "CLEAN" : "INFECTED", result);

                return isClean;

            } finally {
                // Clean up temp file
                if (!tempFile.delete()) {
                    log.warn("Failed to delete temp file: {}", tempFile.getAbsolutePath());
                }
            }

        } catch (Exception e) {
            log.error("Virus scanning failed for file: {}", fileKey, e);

            // If scanning fails, we have options:
            // 1. Reject file (safe but strict)
            // 2. Allow file with warning (business decision)
            // For security, we'll reject the file
            return false;
        }
    }

    private File downloadFileToTemp(String fileKey) throws IOException {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Config.getBucketName())
                .key(fileKey)
                .build();

        File tempFile = File.createTempFile("scan-", ".tmp");
        tempFile.deleteOnExit();

        s3Client.getObject(getObjectRequest, ResponseTransformer.toFile(tempFile));

        return tempFile;
    }

    /**
     * Alternative: Use AWS S3 Object Lambda for scanning
     * This is more scalable but requires additional setup
     */
    public boolean scanFileWithLambda(String fileKey) {
        // Implementation using AWS Lambda with ClamAV
        // This is the recommended approach for production

        log.info("Scanning file with Lambda: {}", fileKey);
        // TODO: Implement AWS Lambda integration

        return true; // Placeholder
    }
}