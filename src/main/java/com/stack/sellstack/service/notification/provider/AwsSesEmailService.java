package com.stack.sellstack.service.notification.provider;

import com.stack.sellstack.config.properties.EmailProperties;
import com.stack.sellstack.exception.NotificationException;
import com.stack.sellstack.model.dto.request.EmailQueueRequest;
import com.stack.sellstack.model.dto.request.EmailRequest;
import com.stack.sellstack.model.dto.response.EmailResponse;
import com.stack.sellstack.model.dto.response.EmailStatusResponse;
import com.stack.sellstack.model.dto.response.ProcessedTemplate;
import com.stack.sellstack.model.entity.EmailQueue;
import com.stack.sellstack.repository.EmailQueueRepository;
import com.stack.sellstack.service.notification.EmailService;
import com.stack.sellstack.service.notification.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class AwsSesEmailService implements EmailService {

    private final SesClient sesClient;
    private final EmailProperties emailProperties;
    private final EmailQueueRepository emailQueueRepository;
    private final EmailTemplateService emailTemplateService;

    @Override
    public CompletableFuture<EmailResponse> sendEmail(EmailRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate request
                validateEmailRequest(request);

                // Check rate limits
                checkRateLimits(request.getSellerId());

                // Prepare SES request
                SendEmailRequest sesRequest = createSesRequest(request);

                // Send email
                SendEmailResponse response = sesClient.sendEmail(sesRequest);

                log.info("Email sent via AWS SES. MessageId: {}, To: {}",
                        response.messageId(), request.getTo());

                // Save to queue for tracking
                EmailQueue emailQueue = saveToQueue(request, response.messageId(), "AWS_SES");

                return EmailResponse.builder()
                        .emailId(emailQueue.getId().toString())
                        .messageId(response.messageId())
                        .status("SENT")
                        .sentAt(new Date())
                        .provider("AWS_SES")
                        .build();

            } catch (Exception e) {
                log.error("Failed to send email via AWS SES", e);
                throw new NotificationException("Failed to send email: " + e.getMessage());
            }
        });
    }

    @Override
    @Transactional
    public String queueEmail(EmailQueueRequest request) {
        try {
            // Process template if templateKey is provided
            String htmlContent = request.getHtmlContent();
            String subject = request.getSubject();

            if (request.getTemplateKey() != null) {
                ProcessedTemplate processed = emailTemplateService.processTemplate(
                        request.getTemplateKey(),
                        request.getVariables()
                );
                htmlContent = processed.getHtmlContent();
                subject = processed.getSubject();

                if (request.getPlainTextContent() == null) {
                    // For simplicity, create a new request with updated plain text
                    EmailQueueRequest updatedRequest = EmailQueueRequest.builder()
                            .sellerId(request.getSellerId())
                            .orderId(request.getOrderId())
                            .templateKey(request.getTemplateKey())
                            .to(request.getTo())
                            .toName(request.getToName())
                            .cc(request.getCc())
                            .bcc(request.getBcc())
                            .subject(subject)
                            .htmlContent(htmlContent)
                            .plainTextContent(processed.getPlainTextContent())
                            .attachments(request.getAttachments())
                            .variables(request.getVariables())
                            .priority(request.getPriority())
                            .sendAfter(request.getSendAfter())
                            .build();
                    request = updatedRequest;
                }
            }

            // Create email queue entry
            EmailQueue emailQueue = EmailQueue.builder()
                    .sellerId(request.getSellerId())
                    .orderId(request.getOrderId())
                    .templateKey(request.getTemplateKey())
                    .toEmail(request.getTo())
                    .toName(request.getToName())
                    .ccEmails(request.getCc())
                    .bccEmails(request.getBcc())
                    .subject(subject)
                    .htmlContent(htmlContent)
                    .plainTextContent(request.getPlainTextContent())
                    .attachments(request.getAttachments())
                    .variables(request.getVariables())
                    .priority(request.getPriority() != null ? request.getPriority() : 5)
                    .sendAfter(request.getSendAfter() != null ? request.getSendAfter() : new Date())
                    .maxRetries(3)
                    .retryCount(0)
                    .status("PENDING")
                    .build();

            emailQueue = emailQueueRepository.save(emailQueue);

            log.info("Email queued with ID: {}, To: {}",
                    emailQueue.getId(), request.getTo());

            return emailQueue.getId().toString();

        } catch (Exception e) {
            log.error("Failed to queue email", e);
            throw new NotificationException("Failed to queue email: " + e.getMessage());
        }
    }

    @Override
    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
    @Transactional
    public void processEmailQueue() {
        Date currentTime = new Date();
        List<EmailQueue> pendingEmails = emailQueueRepository
                .findPendingEmails(currentTime, 100); // Process 100 at a time

        log.info("Processing {} pending emails from queue", pendingEmails.size());

        for (EmailQueue emailQueue : pendingEmails) {
            try {
                // Update status to PROCESSING
                emailQueue.setStatus("PROCESSING");
                emailQueueRepository.save(emailQueue);

                // Prepare and send email
                EmailRequest emailRequest = EmailRequest.builder()
                        .sellerId(emailQueue.getSellerId())
                        .to(emailQueue.getToEmail())
                        .toName(emailQueue.getToName())
                        .subject(emailQueue.getSubject())
                        .htmlContent(emailQueue.getHtmlContent())
                        .plainTextContent(emailQueue.getPlainTextContent())
                        .cc(emailQueue.getCcEmails())
                        .bcc(emailQueue.getBccEmails())
                        .attachments(emailQueue.getAttachments())
                        .build();

                // Send email
                SendEmailRequest sesRequest = createSesRequest(emailRequest);
                SendEmailResponse response = sesClient.sendEmail(sesRequest);

                // Update queue with success
                emailQueue.setStatus("SENT");
                emailQueue.setProvider("AWS_SES");
                emailQueue.setProviderMessageId(response.messageId());
                emailQueue.setSentAt(new Date());
                emailQueue.setRetryCount(0);
                emailQueueRepository.save(emailQueue);

                log.debug("Successfully sent queued email ID: {}", emailQueue.getId());

            } catch (Exception e) {
                log.error("Failed to process queued email ID: {}", emailQueue.getId(), e);

                // Update retry count
                emailQueue.setRetryCount(emailQueue.getRetryCount() != null ?
                        emailQueue.getRetryCount() + 1 : 1);

                if (emailQueue.getRetryCount() >= emailQueue.getMaxRetries()) {
                    emailQueue.setStatus("FAILED");
                    emailQueue.setProviderResponse(e.getMessage());
                } else {
                    // Schedule retry with exponential backoff
                    Calendar retryTime = Calendar.getInstance();
                    retryTime.add(Calendar.MINUTE,
                            (int) Math.pow(2, emailQueue.getRetryCount()));
                    emailQueue.setSendAfter(retryTime.getTime());
                    emailQueue.setStatus("PENDING");
                }

                emailQueueRepository.save(emailQueue);
            }
        }
    }

    private SendEmailRequest createSesRequest(EmailRequest request) {
        // Build destination
        Destination destination = Destination.builder()
                .toAddresses(request.getTo())
                .ccAddresses(request.getCc() != null ? Arrays.asList(request.getCc()) : null)
                .bccAddresses(request.getBcc() != null ? Arrays.asList(request.getBcc()) : null)
                .build();

        // Build message
        Content subject = Content.builder()
                .charset(StandardCharsets.UTF_8.name())
                .data(request.getSubject())
                .build();

        Body.Builder bodyBuilder = Body.builder();

        if (request.getHtmlContent() != null) {
            Content htmlContent = Content.builder()
                    .charset(StandardCharsets.UTF_8.name())
                    .data(request.getHtmlContent())
                    .build();
            bodyBuilder.html(htmlContent);
        }

        if (request.getPlainTextContent() != null) {
            Content textContent = Content.builder()
                    .charset(StandardCharsets.UTF_8.name())
                    .data(request.getPlainTextContent())
                    .build();
            bodyBuilder.text(textContent);
        }

        Body body = bodyBuilder.build();

        Message message = Message.builder()
                .subject(subject)
                .body(body)
                .build();

        // Build send request
        SendEmailRequest.Builder sendRequestBuilder = SendEmailRequest.builder()
                .source(emailProperties.getFromEmail())
                .destination(destination)
                .message(message);

        // Add source ARN if configured
        if (emailProperties.getSourceArn() != null && !emailProperties.getSourceArn().isEmpty()) {
            sendRequestBuilder.sourceArn(emailProperties.getSourceArn());
        }

        // Add configuration set for tracking
        if (emailProperties.getConfigurationSet() != null &&
                !emailProperties.getConfigurationSet().isEmpty()) {
            sendRequestBuilder.configurationSetName(emailProperties.getConfigurationSet());
        }

        // Add reply-to address
        if (emailProperties.getReplyTo() != null && !emailProperties.getReplyTo().isEmpty()) {
            sendRequestBuilder.replyToAddresses(emailProperties.getReplyTo());
        }

        return sendRequestBuilder.build();
    }

    private void validateEmailRequest(EmailRequest request) {
        if (request.getTo() == null || request.getTo().isEmpty()) {
            throw new NotificationException("Recipient email is required");
        }

        if (request.getSubject() == null || request.getSubject().isEmpty()) {
            throw new NotificationException("Email subject is required");
        }

        if (request.getHtmlContent() == null && request.getPlainTextContent() == null) {
            throw new NotificationException("Email content is required");
        }

        // Validate email format
        if (!isValidEmail(request.getTo())) {
            throw new NotificationException("Invalid recipient email format");
        }
    }

    private void checkRateLimits(String sellerId) {
        // TODO: Implement rate limiting per seller
        // Check daily email limit from communication_preferences table
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email != null && email.matches(emailRegex);
    }

    private EmailQueue saveToQueue(EmailRequest request, String messageId, String provider) {
        EmailQueue emailQueue = EmailQueue.builder()
                .sellerId(request.getSellerId())
                .toEmail(request.getTo())
                .toName(request.getToName())
                .subject(request.getSubject())
                .htmlContent(request.getHtmlContent())
                .plainTextContent(request.getPlainTextContent())
                .ccEmails(request.getCc())
                .bccEmails(request.getBcc())
                .attachments(request.getAttachments())
                .status("SENT")
                .provider(provider)
                .providerMessageId(messageId)
                .sentAt(new Date())
                .openCount(0)
                .clickCount(0)
                .build();

        return emailQueueRepository.save(emailQueue);
    }

    @Override
    public EmailStatusResponse getEmailStatus(String emailId) {
        EmailQueue emailQueue = emailQueueRepository.findById(java.util.UUID.fromString(emailId))
                .orElseThrow(() -> new NotificationException("Email not found"));

        return EmailStatusResponse.builder()
                .emailId(emailQueue.getId().toString())
                .status(emailQueue.getStatus())
                .provider(emailQueue.getProvider())
                .messageId(emailQueue.getProviderMessageId())
                .sentAt(emailQueue.getSentAt())
                .deliveredAt(emailQueue.getDeliveredAt())
                .openedAt(emailQueue.getOpenedAt())
                .openedCount(emailQueue.getOpenCount())
                .clickedCount(emailQueue.getClickCount())
                .bounceReason(emailQueue.getBounceReason())
                .bounceType(emailQueue.getBounceType())
                .build();
    }

    @Override
    @Transactional
    public void handleBounce(String messageId, String bounceType, String reason) {
        Optional<EmailQueue> emailQueueOpt = emailQueueRepository
                .findByProviderMessageId(messageId);

        if (emailQueueOpt.isPresent()) {
            EmailQueue emailQueue = emailQueueOpt.get();
            emailQueue.setStatus("BOUNCED");
            emailQueue.setBounceType(bounceType);
            emailQueue.setBounceReason(reason);
            emailQueueRepository.save(emailQueue);

            log.warn("Email bounced. MessageId: {}, Reason: {}", messageId, reason);
        }
    }

    @Override
    @Transactional
    public void handleDelivery(String messageId) {
        Optional<EmailQueue> emailQueueOpt = emailQueueRepository
                .findByProviderMessageId(messageId);

        if (emailQueueOpt.isPresent()) {
            EmailQueue emailQueue = emailQueueOpt.get();
            emailQueue.setStatus("DELIVERED");
            emailQueue.setDeliveredAt(new Date());
            emailQueueRepository.save(emailQueue);

            log.info("Email delivered. MessageId: {}", messageId);
        }
    }

    @Override
    @Transactional
    public void handleOpen(String messageId) {
        Optional<EmailQueue> emailQueueOpt = emailQueueRepository
                .findByProviderMessageId(messageId);

        if (emailQueueOpt.isPresent()) {
            EmailQueue emailQueue = emailQueueOpt.get();
            emailQueue.setOpenedAt(new Date());
            emailQueue.setOpenCount(emailQueue.getOpenCount() != null ?
                    emailQueue.getOpenCount() + 1 : 1);
            emailQueueRepository.save(emailQueue);

            log.debug("Email opened. MessageId: {}", messageId);
        }
    }

    @Override
    @Transactional
    public void handleClick(String messageId, String clickUrl) {
        Optional<EmailQueue> emailQueueOpt = emailQueueRepository
                .findByProviderMessageId(messageId);

        if (emailQueueOpt.isPresent()) {
            EmailQueue emailQueue = emailQueueOpt.get();
            emailQueue.setClickedAt(new Date());
            emailQueue.setClickCount(emailQueue.getClickCount() != null ?
                    emailQueue.getClickCount() + 1 : 1);
            emailQueueRepository.save(emailQueue);

            log.debug("Email clicked. MessageId: {}, URL: {}", messageId, clickUrl);
        }
    }

    @Override
    public void handleSesWebhook(Map<String, Object> payload) {
        // TODO: Implement AWS SES webhook handling
        log.info("Received SES webhook: {}", payload);
    }

    @Override
    public void handleSendGridWebhook(Map<String, Object> payload) {
        // TODO: Implement SendGrid webhook handling
        log.info("Received SendGrid webhook: {}", payload);
    }
}