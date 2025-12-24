package com.stack.sellstack.service.notification.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stack.sellstack.config.properties.WhatsAppProperties;
import com.stack.sellstack.exception.NotificationException;
import com.stack.sellstack.model.dto.request.WhatsAppQueueRequest;
import com.stack.sellstack.model.dto.request.WhatsAppRequest;
import com.stack.sellstack.model.dto.response.WhatsAppResponse;
import com.stack.sellstack.model.dto.response.WhatsAppStatusResponse;
import com.stack.sellstack.model.entity.WhatsAppQueue;
import com.stack.sellstack.model.entity.WhatsAppTemplate;
import com.stack.sellstack.repository.WhatsAppQueueRepository;
import com.stack.sellstack.repository.WhatsAppTemplateRepository;
import com.stack.sellstack.service.notification.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class MetaWhatsAppService implements WhatsAppService {

    private final WhatsAppProperties whatsAppProperties;
    private final WhatsAppTemplateRepository whatsAppTemplateRepository;
    private final WhatsAppQueueRepository whatsAppQueueRepository;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    @Override
    public CompletableFuture<WhatsAppResponse> sendMessage(WhatsAppRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                validateWhatsAppRequest(request);
                checkWhatsAppRateLimits(request.getSellerId());

                String response;
                String messageId;

                if (request.getTemplateKey() != null) {
                    response = sendTemplateMessage(request);
                } else if (request.getMediaUrl() != null ||
                        (request.getMediaUrls() != null && !request.getMediaUrls().isEmpty())) {
                    response = sendMediaMessage(request);
                } else {
                    response = sendTextMessage(request);
                }

                messageId = extractMessageId(response);

                log.info("WhatsApp message sent. MessageId: {}, To: {}", messageId, request.getToPhone());

                WhatsAppQueue whatsAppQueue = saveToQueue(request, messageId, response);

                return WhatsAppResponse.builder()
                        .messageId(whatsAppQueue.getId().toString())
                        .providerMessageId(messageId)
                        .status("SENT")
                        .sentAt(new Date())
                        .provider("META")
                        .build();

            } catch (Exception e) {
                log.error("Failed to send WhatsApp message", e);
                throw new NotificationException("Failed to send WhatsApp message: " + e.getMessage());
            }
        });
    }

    @Override
    @Transactional
    public String queueMessage(WhatsAppQueueRequest request) {
        try {
            if (request.getTemplateKey() != null) {
                WhatsAppTemplate template = whatsAppTemplateRepository
                        .findByTemplateKeyAndStatus(request.getTemplateKey(), "APPROVED")
                        .orElseThrow(() -> new NotificationException("WhatsApp template not found or not approved"));

                request.setWhatsAppTemplateId(template.getWhatsappTemplateId());
            }

            WhatsAppQueue whatsAppQueue = WhatsAppQueue.builder()
                    .sellerId(request.getSellerId())
                    .orderId(request.getOrderId())
                    .templateKey(request.getTemplateKey())
                    .whatsappTemplateId(request.getWhatsAppTemplateId())
                    .toPhone(request.getToPhone())
                    .toName(request.getToName())
                    .variables(request.getVariables())
                    .mediaUrls(request.getMediaUrls())
                    .priority(request.getPriority() != null ? request.getPriority() : 5)
                    .sendAfter(request.getSendAfter() != null ? request.getSendAfter() : new Date())
                    .maxRetries(3)
                    .retryCount(0)
                    .status("PENDING")
                    .build();

            whatsAppQueue = whatsAppQueueRepository.save(whatsAppQueue);

            log.info("WhatsApp message queued with ID: {}, To: {}",
                    whatsAppQueue.getId(), request.getToPhone());

            return whatsAppQueue.getId().toString();

        } catch (Exception e) {
            log.error("Failed to queue WhatsApp message", e);
            throw new NotificationException("Failed to queue WhatsApp message: " + e.getMessage());
        }
    }

    @Override
    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
    @Transactional
    public void processMessageQueue() {
        Date currentTime = new Date();
        Pageable limit = PageRequest.of(0, 100);
        List<WhatsAppQueue> pendingMessages = whatsAppQueueRepository
                .findPendingMessagesWithLimit(currentTime, limit);

        log.info("Processing {} pending WhatsApp messages from queue", pendingMessages.size());

        for (WhatsAppQueue whatsAppQueue : pendingMessages) {
            try {
                whatsAppQueue.setStatus("PROCESSING");
                whatsAppQueueRepository.save(whatsAppQueue);

                WhatsAppRequest whatsAppRequest = WhatsAppRequest.builder()
                        .sellerId(whatsAppQueue.getSellerId())
                        .toPhone(whatsAppQueue.getToPhone())
                        .toName(whatsAppQueue.getToName())
                        .templateKey(whatsAppQueue.getTemplateKey())
                        .variables(whatsAppQueue.getVariables())
                        .mediaUrls(whatsAppQueue.getMediaUrls())
                        .build();

                String response;

                if (whatsAppQueue.getTemplateKey() != null) {
                    response = sendTemplateMessage(whatsAppRequest);
                } else if (whatsAppQueue.getMediaUrls() != null && !whatsAppQueue.getMediaUrls().isEmpty()) {
                    response = sendMediaMessage(whatsAppRequest);
                } else {
                    String messageText = whatsAppQueue.getVariables() != null ?
                            whatsAppQueue.getVariables().getOrDefault("message", "").toString() : "";
                    whatsAppRequest.setMessage(messageText);
                    response = sendTextMessage(whatsAppRequest);
                }

                String messageId = extractMessageId(response);

                whatsAppQueue.setStatus("SENT");
                whatsAppQueue.setProvider("META");
                whatsAppQueue.setProviderMessageId(messageId);
                whatsAppQueue.setProviderResponse(response);
                whatsAppQueue.setSentAt(new Date());
                whatsAppQueue.setRetryCount(0);
                whatsAppQueueRepository.save(whatsAppQueue);

                log.debug("Successfully sent queued WhatsApp message ID: {}", whatsAppQueue.getId());

            } catch (Exception e) {
                log.error("Failed to process queued WhatsApp message ID: {}", whatsAppQueue.getId(), e);

                whatsAppQueue.setRetryCount(whatsAppQueue.getRetryCount() != null ?
                        whatsAppQueue.getRetryCount() + 1 : 1);

                if (whatsAppQueue.getRetryCount() >= whatsAppQueue.getMaxRetries()) {
                    whatsAppQueue.setStatus("FAILED");
                    whatsAppQueue.setProviderResponse(e.getMessage());
                    whatsAppQueue.setErrorCode("RETRY_EXCEEDED");
                    whatsAppQueue.setErrorMessage(e.getMessage());
                } else {
                    Calendar retryTime = Calendar.getInstance();
                    retryTime.add(Calendar.MINUTE,
                            (int) Math.pow(2, whatsAppQueue.getRetryCount()));
                    whatsAppQueue.setSendAfter(retryTime.getTime());
                    whatsAppQueue.setStatus("PENDING");
                }

                whatsAppQueueRepository.save(whatsAppQueue);
            }
        }
    }

    private String sendTemplateMessage(WhatsAppRequest request) throws IOException {
        WhatsAppTemplate template = whatsAppTemplateRepository
                .findByTemplateKeyAndStatus(request.getTemplateKey(), "APPROVED")
                .orElseThrow(() -> new NotificationException("Template not approved"));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("messaging_product", "whatsapp");
        requestBody.put("to", request.getToPhone());
        requestBody.put("type", "template");

        Map<String, Object> templateMap = new HashMap<>();
        templateMap.put("name", template.getWhatsappTemplateId());

        Map<String, String> languageMap = new HashMap<>();
        languageMap.put("code", template.getLanguageCode() != null ? template.getLanguageCode() : "en");
        templateMap.put("language", languageMap);

        if (request.getVariables() != null && !request.getVariables().isEmpty()) {
            List<Map<String, Object>> components = new ArrayList<>();

            if (request.getVariables().containsKey("body_variables")) {
                List<Map<String, String>> bodyVariables = new ArrayList<>();
                Object bodyVarsObj = request.getVariables().get("body_variables");

                if (bodyVarsObj instanceof List) {
                    List<?> bodyVars = (List<?>) bodyVarsObj;
                    for (Object var : bodyVars) {
                        Map<String, String> varMap = new HashMap<>();
                        varMap.put("type", "text");
                        varMap.put("text", var.toString());
                        bodyVariables.add(varMap);
                    }

                    Map<String, Object> bodyComponent = new HashMap<>();
                    bodyComponent.put("type", "body");
                    bodyComponent.put("parameters", bodyVariables);
                    components.add(bodyComponent);
                }
            }

            if (!components.isEmpty()) {
                templateMap.put("components", components);
            }
        }

        requestBody.put("template", templateMap);

        return makeWhatsAppApiCall(requestBody);
    }

    private String sendTextMessage(WhatsAppRequest request) throws IOException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("messaging_product", "whatsapp");
        requestBody.put("to", request.getToPhone());
        requestBody.put("type", "text");

        Map<String, String> textMap = new HashMap<>();
        textMap.put("preview_url", "false");
        textMap.put("body", request.getMessage() != null ? request.getMessage() : "");

        requestBody.put("text", textMap);

        return makeWhatsAppApiCall(requestBody);
    }

    private String sendMediaMessage(WhatsAppRequest request) throws IOException {
        String mediaUrl;
        String mediaType;

        if (request.getMediaUrl() != null) {
            mediaUrl = request.getMediaUrl();
            mediaType = "image"; // default, you might want to detect from URL
        } else if (request.getMediaUrls() != null && !request.getMediaUrls().isEmpty()) {
            Map<String, Object> media = request.getMediaUrls().getFirst();
            mediaUrl = media.get("url").toString();
            mediaType = media.get("type") != null ? media.get("type").toString() : "image";
        } else {
            throw new NotificationException("No media URL provided");
        }

        String mediaId = uploadMedia(mediaUrl, mediaType);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("messaging_product", "whatsapp");
        requestBody.put("to", request.getToPhone());
        requestBody.put("type", mediaType.toLowerCase());

        Map<String, String> mediaMap = new HashMap<>();
        mediaMap.put("id", mediaId);

        if (request.getMediaUrls() != null && !request.getMediaUrls().isEmpty() &&
                request.getMediaUrls().getFirst().containsKey("caption")) {
            mediaMap.put("caption", request.getMediaUrls().getFirst().get("caption").toString());
        }

        requestBody.put(mediaType.toLowerCase(), mediaMap);

        return makeWhatsAppApiCall(requestBody);
    }

    private String uploadMedia(String mediaUrl, String mediaType) throws IOException {
        String uploadUrl = whatsAppProperties.getBaseUrl() +
                whatsAppProperties.getPhoneNumberId() + "/media";

        Map<String, Object> uploadBody = new HashMap<>();
        uploadBody.put("messaging_product", "whatsapp");
        uploadBody.put("type", mediaType);
        uploadBody.put("url", mediaUrl);

        RequestBody body = RequestBody.create(
                objectMapper.writeValueAsString(uploadBody),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(uploadUrl)
                .post(body)
                .addHeader("Authorization", "Bearer " + whatsAppProperties.getAccessToken())
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                assert response.body() != null;
                throw new NotificationException("Failed to upload media: " + response.body().string());
            }

            assert response.body() != null;
            Map<String, Object> responseMap = objectMapper.readValue(
                    response.body().string(), Map.class);

            return (String) responseMap.get("id");
        }
    }

    private String makeWhatsAppApiCall(Map<String, Object> requestBody) throws IOException {
        String url = whatsAppProperties.getBaseUrl() +
                whatsAppProperties.getPhoneNumberId() + "/messages";

        RequestBody body = RequestBody.create(
                objectMapper.writeValueAsString(requestBody),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer " + whatsAppProperties.getAccessToken())
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                assert response.body() != null;
                String errorBody = response.body().string();
                log.error("WhatsApp API error: {}", errorBody);
                throw new NotificationException("WhatsApp API error: " + errorBody);
            }

            assert response.body() != null;
            return response.body().string();
        }
    }

    private String extractMessageId(String response) throws IOException {
        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
        if (responseMap.containsKey("messages")) {
            Map<String, Object> messages = (Map<String, Object>) responseMap.get("messages");
            return (String) messages.get("id");
        }
        return UUID.randomUUID().toString();
    }

    private void validateWhatsAppRequest(WhatsAppRequest request) {
        if (request.getToPhone() == null || request.getToPhone().isEmpty()) {
            throw new NotificationException("Recipient phone number is required");
        }

        if (!isValidPhoneNumber(request.getToPhone())) {
            throw new NotificationException("Invalid phone number format. Use international format: +919876543210");
        }

        if (request.getTemplateKey() != null && request.getMessage() != null) {
            throw new NotificationException("Cannot specify both templateKey and message");
        }

        if (request.getTemplateKey() == null && request.getMessage() == null &&
                request.getMediaUrl() == null &&
                (request.getMediaUrls() == null || request.getMediaUrls().isEmpty())) {
            throw new NotificationException("Either templateKey, message, or mediaUrl is required");
        }
    }

    private void checkWhatsAppRateLimits(String sellerId) {
        // TODO: Implement rate limiting
    }

    private boolean isValidPhoneNumber(String phone) {
        return phone != null && phone.matches("^\\+[1-9]\\d{1,14}$");
    }

    private WhatsAppQueue saveToQueue(WhatsAppRequest request, String providerMessageId,
                                      String providerResponse) {
        WhatsAppQueue whatsAppQueue = WhatsAppQueue.builder()
                .sellerId(request.getSellerId())
                .toPhone(request.getToPhone())
                .toName(request.getToName())
                .templateKey(request.getTemplateKey())
                .variables(request.getVariables())
                .mediaUrls(request.getMediaUrls())
                .status("SENT")
                .provider("META")
                .providerMessageId(providerMessageId)
                .providerResponse(providerResponse)
                .sentAt(new Date())
                .build();

        return whatsAppQueueRepository.save(whatsAppQueue);
    }

    @Override
    public WhatsAppStatusResponse getMessageStatus(String messageId) {
        WhatsAppQueue whatsAppQueue = whatsAppQueueRepository.findById(UUID.fromString(messageId))
                .orElseThrow(() -> new NotificationException("Message not found"));

        return WhatsAppStatusResponse.builder()
                .messageId(whatsAppQueue.getId().toString())
                .status(whatsAppQueue.getStatus())
                .provider(whatsAppQueue.getProvider())
                .providerMessageId(whatsAppQueue.getProviderMessageId())
                .sentAt(whatsAppQueue.getSentAt())
                .deliveredAt(whatsAppQueue.getDeliveredAt())
                .readAt(whatsAppQueue.getReadAt())
                .errorCode(whatsAppQueue.getErrorCode())
                .errorMessage(whatsAppQueue.getErrorMessage())
                .build();
    }

    @Override
    @Transactional
    public void handleWebhookEvent(Map<String, Object> event) {
        try {
            List<Map<String, Object>> entries = (List<Map<String, Object>>) event.get("entry");

            for (Map<String, Object> entry : entries) {
                List<Map<String, Object>> changes = (List<Map<String, Object>>) entry.get("changes");

                for (Map<String, Object> change : changes) {
                    Map<String, Object> value = (Map<String, Object>) change.get("value");

                    if (value.containsKey("messages")) {
                        handleIncomingMessage(value);
                    }

                    if (value.containsKey("statuses")) {
                        handleStatusUpdate(value);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to process WhatsApp webhook event", e);
        }
    }

    @Override
    public boolean verifyWebhookSignature(Map<String, Object> payload, String signature) {
        try {
            String payloadString = objectMapper.writeValueAsString(payload);
            String expectedSignature = calculateHmacSha256(payloadString, whatsAppProperties.getWebhookSecret());
            return expectedSignature.equals(signature);
        } catch (Exception e) {
            log.error("Failed to verify webhook signature", e);
            return false;
        }
    }

    private String calculateHmacSha256(String data, String secret)
            throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(secretKeySpec);

        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hmacBytes);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private void handleIncomingMessage(Map<String, Object> value) {
        List<Map<String, Object>> messages = (List<Map<String, Object>>) value.get("messages");

        for (Map<String, Object> message : messages) {
            String from = (String) message.get("from");
            String messageId = (String) message.get("id");
            String timestamp = (String) message.get("timestamp");

            log.info("Received WhatsApp message from {}: {}", from, messageId);
            // TODO: Process incoming message
        }
    }

    private void handleStatusUpdate(Map<String, Object> value) {
        List<Map<String, Object>> statuses = (List<Map<String, Object>>) value.get("statuses");

        for (Map<String, Object> status : statuses) {
            String messageId = (String) status.get("id");
            String statusStr = (String) status.get("status");
            String timestamp = (String) status.get("timestamp");

            Optional<WhatsAppQueue> queueOpt = whatsAppQueueRepository
                    .findByProviderMessageId(messageId);

            if (queueOpt.isPresent()) {
                WhatsAppQueue whatsAppQueue = queueOpt.get();
                whatsAppQueue.setStatus(statusStr.toUpperCase());

                try {
                    long timestampMs = Long.parseLong(timestamp) * 1000;
                    if ("delivered".equals(statusStr)) {
                        whatsAppQueue.setDeliveredAt(new Date(timestampMs));
                    } else if ("read".equals(statusStr)) {
                        whatsAppQueue.setReadAt(new Date(timestampMs));
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid timestamp in WhatsApp status update: {}", timestamp);
                }

                whatsAppQueueRepository.save(whatsAppQueue);
                log.debug("Updated WhatsApp message status: {} -> {}", messageId, statusStr);
            }
        }
    }
}