package com.stack.sellstack.controller.notification;

import com.stack.sellstack.model.dto.request.*;
import com.stack.sellstack.model.dto.response.*;
import com.stack.sellstack.service.notification.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Notifications", description = "Email and WhatsApp notification APIs")
public class NotificationController {

    private final EmailService emailService;
    private final WhatsAppService whatsAppService;
    private final EmailTemplateService emailTemplateService;
    private final WhatsAppTemplateService whatsAppTemplateService;
    private final DeliveryAnalyticsService analyticsService;

    // ========== EMAIL ENDPOINTS ==========

    @PostMapping("/email/send")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    @Operation(summary = "Send email immediately",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<EmailResponse>> sendEmail(
            @Valid @RequestBody EmailRequest request) {

        CompletableFuture<EmailResponse> future = emailService.sendEmail(request);

        return ResponseEntity.accepted().body(ApiResponse.success(
                future.join(), // In production, use async handling
                "Email sending initiated"
        ));
    }

    @PostMapping("/email/queue")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    @Operation(summary = "Queue email for delivery",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<QueueResponse>> queueEmail(
            @Valid @RequestBody EmailQueueRequest request) {

        String queueId = emailService.queueEmail(request);

        return ResponseEntity.ok(ApiResponse.success(
                QueueResponse.builder()
                        .queueId(queueId)
                        .status("QUEUED")
                        .message("Email queued for delivery")
                        .build(),
                "Email queued successfully"
        ));
    }

    @GetMapping("/email/status/{emailId}")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    @Operation(summary = "Get email delivery status",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<EmailStatusResponse>> getEmailStatus(
            @PathVariable String emailId) {

        EmailStatusResponse status = emailService.getEmailStatus(emailId);

        return ResponseEntity.ok(ApiResponse.success(
                status,
                "Email status retrieved"
        ));
    }

    // ========== WHATSAPP ENDPOINTS ==========

    @PostMapping("/whatsapp/send")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    @Operation(summary = "Send WhatsApp message immediately",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<WhatsAppResponse>> sendWhatsApp(
            @Valid @RequestBody WhatsAppRequest request) {

        CompletableFuture<WhatsAppResponse> future = whatsAppService.sendMessage(request);

        return ResponseEntity.accepted().body(ApiResponse.success(
                future.join(),
                "WhatsApp message sending initiated"
        ));
    }

    @PostMapping("/whatsapp/queue")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    @Operation(summary = "Queue WhatsApp message for delivery",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<QueueResponse>> queueWhatsApp(
            @Valid @RequestBody WhatsAppQueueRequest request) {

        String queueId = whatsAppService.queueMessage(request);

        return ResponseEntity.ok(ApiResponse.success(
                QueueResponse.builder()
                        .queueId(queueId)
                        .status("QUEUED")
                        .message("WhatsApp message queued for delivery")
                        .build(),
                "WhatsApp message queued successfully"
        ));
    }

    @GetMapping("/whatsapp/status/{messageId}")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    @Operation(summary = "Get WhatsApp message delivery status",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<WhatsAppStatusResponse>> getWhatsAppStatus(
            @PathVariable String messageId) {

        WhatsAppStatusResponse status = whatsAppService.getMessageStatus(messageId);

        return ResponseEntity.ok(ApiResponse.success(
                status,
                "WhatsApp message status retrieved"
        ));
    }

    // ========== TEMPLATE MANAGEMENT ==========

    @PostMapping("/email/templates")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create or update email template",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<EmailTemplateResponse>> createEmailTemplate(
            @Valid @RequestBody EmailTemplateRequest request) {

        EmailTemplateResponse template = emailTemplateService.createOrUpdateTemplate(request);

        return ResponseEntity.ok(ApiResponse.success(
                template,
                "Email template saved successfully"
        ));
    }

    @GetMapping("/email/templates")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    @Operation(summary = "Get all email templates",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<EmailTemplateResponse>>> getEmailTemplates(
            @RequestParam(required = false) String category) {

        List<EmailTemplateResponse> templates = emailTemplateService.getAllTemplates(category);

        return ResponseEntity.ok(ApiResponse.success(
                templates,
                "Email templates retrieved"
        ));
    }

    @PostMapping("/whatsapp/templates")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create or update WhatsApp template",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<WhatsAppTemplateResponse>> createWhatsAppTemplate(
            @Valid @RequestBody WhatsAppTemplateRequest request) {

        WhatsAppTemplateResponse template = whatsAppTemplateService.createOrUpdateTemplate(request);

        return ResponseEntity.ok(ApiResponse.success(
                template,
                "WhatsApp template saved successfully"
        ));
    }

    // ========== WEBHOOK ENDPOINTS ==========

    @PostMapping("/webhook/email")
    @Operation(summary = "Handle email provider webhooks (AWS SES, SendGrid)")
    public ResponseEntity<Void> handleEmailWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader Map<String, String> headers) {

        // Verify webhook signature (implement based on provider)
        String provider = determineProviderFromHeaders(headers);

        switch (provider) {
            case "AWS_SES":
                emailService.handleSesWebhook(payload);
                break;
            case "SENDGRID":
                emailService.handleSendGridWebhook(payload);
                break;
            default:
                log.warn("Unknown email provider: {}", provider);
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("/webhook/whatsapp")
    @Operation(summary = "Handle WhatsApp Business API webhooks")
    public ResponseEntity<Void> handleWhatsAppWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader("X-Hub-Signature-256") String signature) {

        // Verify webhook signature
        if (!whatsAppService.verifyWebhookSignature(payload, signature)) {
            log.warn("Invalid WhatsApp webhook signature");
            return ResponseEntity.status(401).build();
        }

        // Handle webhook
        whatsAppService.handleWebhookEvent(payload);

        return ResponseEntity.ok().build();
    }

    // ========== ANALYTICS ==========

    @GetMapping("/analytics/daily")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get daily delivery analytics",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<DailyAnalyticsResponse>> getDailyAnalytics(
            @RequestParam String date,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String sellerId) {

        DailyAnalyticsResponse analytics = analyticsService.getDailyAnalytics(
                date, channel, sellerId);

        return ResponseEntity.ok(ApiResponse.success(
                analytics,
                "Daily analytics retrieved"
        ));
    }

    private String determineProviderFromHeaders(Map<String, String> headers) {
        // Implement logic to determine email provider from headers
        if (headers.containsKey("X-Amz-Sns-Message-Type")) {
            return "AWS_SES";
        } else if (headers.containsKey("X-Twilio-Email-Event-Webhook-Signature")) {
            return "SENDGRID";
        }
        return "UNKNOWN";
    }

    @GetMapping("/whatsapp/templates")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    @Operation(summary = "Get all WhatsApp templates",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<WhatsAppTemplateResponse>>> getWhatsAppTemplates(
            @RequestParam(required = false) String category) {

        List<WhatsAppTemplateResponse> templates = whatsAppTemplateService.getAllTemplates(category);

        return ResponseEntity.ok(ApiResponse.success(
                templates,
                "WhatsApp templates retrieved"
        ));
    }

    @GetMapping("/whatsapp/templates/{templateKey}")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    @Operation(summary = "Get WhatsApp template by key",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<WhatsAppTemplateResponse>> getWhatsAppTemplate(
            @PathVariable String templateKey) {

        WhatsAppTemplateResponse template = whatsAppTemplateService.getTemplateByKey(templateKey);

        return ResponseEntity.ok(ApiResponse.success(
                template,
                "WhatsApp template retrieved"
        ));
    }

    @DeleteMapping("/whatsapp/templates/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete WhatsApp template",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> deleteWhatsAppTemplate(
            @PathVariable String id) {

        whatsAppTemplateService.deleteTemplate(id);

        return ResponseEntity.ok(ApiResponse.success(
                null,
                "WhatsApp template deleted successfully"
        ));
    }

    @PutMapping("/whatsapp/templates/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update WhatsApp template status",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<WhatsAppTemplateResponse>> updateWhatsAppTemplateStatus(
            @PathVariable String id,
            @RequestParam String status,
            @RequestParam(required = false) String rejectionReason) {

        WhatsAppTemplateResponse template = whatsAppTemplateService.updateStatus(id, status, rejectionReason);

        return ResponseEntity.ok(ApiResponse.success(
                template,
                "WhatsApp template status updated"
        ));
    }

    @GetMapping("/analytics/summary")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get analytics summary for a date range",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<DailyAnalyticsResponse>> getAnalyticsSummary(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String sellerId) {

        LocalDate start = LocalDate.parse(startDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDate end = LocalDate.parse(endDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        DailyAnalyticsResponse analytics = analyticsService.getAnalyticsSummary(start, end, sellerId);

        return ResponseEntity.ok(ApiResponse.success(
                analytics,
                "Analytics summary retrieved"
        ));
    }

    @GetMapping("/analytics/delivery-time")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get average delivery time",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAverageDeliveryTime(
            @RequestParam(defaultValue = "ALL") String channel,
            @RequestParam(required = false) String sellerId) {

        double avgTime = analyticsService.getAverageDeliveryTime(channel, sellerId);

        return ResponseEntity.ok(ApiResponse.success(
                Map.of(
                        "channel", channel,
                        "sellerId", sellerId,
                        "averageDeliveryTimeSeconds", avgTime,
                        "averageDeliveryTimeMinutes", Math.round(avgTime / 60 * 100.0) / 100.0
                ),
                "Average delivery time calculated"
        ));
    }
}