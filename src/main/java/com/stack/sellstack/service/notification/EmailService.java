package com.stack.sellstack.service.notification;

import com.stack.sellstack.model.dto.request.EmailQueueRequest;
import com.stack.sellstack.model.dto.request.EmailRequest;
import com.stack.sellstack.model.dto.response.EmailResponse;
import com.stack.sellstack.model.dto.response.EmailStatusResponse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface EmailService {

    /**
     * Send email immediately (fire and forget)
     */
    CompletableFuture<EmailResponse> sendEmail(EmailRequest request);

    /**
     * Queue email for delivery (recommended for bulk)
     */
    String queueEmail(EmailQueueRequest request);

    /**
     * Process pending emails in queue
     */
    void processEmailQueue();

    /**
     * Get email delivery status
     */
    EmailStatusResponse getEmailStatus(String emailId);

    /**
     * Handle email bounce notifications (for webhooks)
     */
    void handleBounce(String messageId, String bounceType, String reason);

    /**
     * Handle email delivery notifications
     */
    void handleDelivery(String messageId);

    /**
     * Handle email open tracking
     */
    void handleOpen(String messageId);

    /**
     * Handle email click tracking
     */
    void handleClick(String messageId, String clickUrl);

    /**
     * Handle AWS SES webhooks
     */
    void handleSesWebhook(Map<String, Object> payload);

    /**
     * Handle SendGrid webhooks
     */
    void handleSendGridWebhook(Map<String, Object> payload);
}