package com.stack.sellstack.service.notification;

import com.stack.sellstack.model.dto.request.WhatsAppQueueRequest;
import com.stack.sellstack.model.dto.request.WhatsAppRequest;
import com.stack.sellstack.model.dto.response.WhatsAppResponse;
import com.stack.sellstack.model.dto.response.WhatsAppStatusResponse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface WhatsAppService {

    /**
     * Send WhatsApp message immediately
     */
    CompletableFuture<WhatsAppResponse> sendMessage(WhatsAppRequest request);

    /**
     * Queue WhatsApp message for delivery
     */
    String queueMessage(WhatsAppQueueRequest request);

    /**
     * Process pending WhatsApp messages in queue
     */
    void processMessageQueue();

    /**
     * Get WhatsApp message delivery status
     */
    WhatsAppStatusResponse getMessageStatus(String messageId);

    /**
     * Handle WhatsApp webhook events
     */
    void handleWebhookEvent(Map<String, Object> event);

    /**
     * Verify webhook signature
     */
    boolean verifyWebhookSignature(Map<String, Object> payload, String signature);
}