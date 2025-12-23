package com.stack.sellstack.service.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class WhatsAppService {

    private final RestTemplate restTemplate;

    @Value("${whatsapp.api.url:}")
    private String whatsappApiUrl;

    @Value("${whatsapp.api.key:}")
    private String whatsappApiKey;

    @Value("${whatsapp.api.template.namespace:}")
    private String templateNamespace;

    @Value("${whatsapp.api.template.payment-success:payment_success}")
    private String paymentSuccessTemplate;

    @Value("${whatsapp.api.template.delivery-ready:delivery_ready}")
    private String deliveryReadyTemplate;

    @Value("${whatsapp.api.template.refund-processed:refund_processed}")
    private String refundProcessedTemplate;

    public WhatsAppService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Send simple WhatsApp message
     */
    public void sendMessage(String phoneNumber, String message) {
        try {
            // Remove any non-digit characters
            String formattedPhone = phoneNumber.replaceAll("[^0-9+]", "");

            // If phone doesn't start with +, add country code (assuming India +91)
            if (!formattedPhone.startsWith("+")) {
                // Remove leading 0 if present
                if (formattedPhone.startsWith("0")) {
                    formattedPhone = formattedPhone.substring(1);
                }
                formattedPhone = "+91" + formattedPhone;
            }

            log.info("Sending WhatsApp message to: {}", formattedPhone);

            // Example for WhatsApp Business API (using template)
            if (whatsappApiUrl != null && !whatsappApiUrl.isEmpty()) {
                sendWhatsAppViaAPI(formattedPhone, message);
            } else {
                // Log message (for development/testing)
                log.info("WhatsApp Message to {}: {}", formattedPhone, message);

                // In production, you would integrate with actual WhatsApp service
                // like Twilio, WhatsApp Business API, or other providers
            }

        } catch (Exception e) {
            log.error("Failed to send WhatsApp message to: {}", phoneNumber, e);
            // Don't throw exception - notification failures shouldn't break main flow
        }
    }

    /**
     * Send WhatsApp message using template (for structured messages)
     */
    public void sendTemplateMessage(String phoneNumber, String templateName,
                                    Map<String, String> parameters) {
        try {
            String formattedPhone = formatPhoneNumber(phoneNumber);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("to", formattedPhone);
            requestBody.put("type", "template");

            Map<String, Object> template = new HashMap<>();
            template.put("name", templateName);

            if (parameters != null && !parameters.isEmpty()) {
                Map<String, Object> language = new HashMap<>();
                language.put("code", "en");

                Map<String, Object> components = new HashMap<>();

                // Add parameters to template
                Map<String, String> params = new HashMap<>();
                parameters.forEach((key, value) -> params.put(key, value));

                components.put("parameters", params);
                template.put("components", components);
                template.put("language", language);
            }

            requestBody.put("template", template);

            sendApiRequest(requestBody);

            log.info("WhatsApp template message sent to: {}", formattedPhone);

        } catch (Exception e) {
            log.error("Failed to send WhatsApp template message to: {}", phoneNumber, e);
        }
    }

    /**
     * Send payment success WhatsApp message
     */
    public void sendPaymentSuccessWhatsApp(String phoneNumber, String productTitle,
                                           String amount, String receiptNumber) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("product_name", productTitle);
        parameters.put("amount", amount);
        parameters.put("receipt_number", receiptNumber);

        sendTemplateMessage(phoneNumber, paymentSuccessTemplate, parameters);
    }

    /**
     * Send product delivery WhatsApp message
     */
    public void sendDeliveryReadyWhatsApp(String phoneNumber, String productTitle,
                                          String downloadLink) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("product_name", productTitle);
        parameters.put("download_link", downloadLink);

        sendTemplateMessage(phoneNumber, deliveryReadyTemplate, parameters);
    }

    /**
     * Send refund processed WhatsApp message
     */
    public void sendRefundProcessedWhatsApp(String phoneNumber, String productTitle,
                                            String refundAmount, String reason) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("product_name", productTitle);
        parameters.put("refund_amount", refundAmount);
        parameters.put("refund_reason", reason);

        sendTemplateMessage(phoneNumber, refundProcessedTemplate, parameters);
    }

    /**
     * Helper method to send API request
     */
    private void sendApiRequest(Map<String, Object> requestBody) {
        if (whatsappApiUrl == null || whatsappApiUrl.isEmpty()) {
            return; // Skip if no API configured
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + whatsappApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    whatsappApiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("WhatsApp API request successful");
            } else {
                log.warn("WhatsApp API request failed: {}", response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("WhatsApp API request failed", e);
        }
    }

    /**
     * Helper method to send WhatsApp via API
     */
    private void sendWhatsAppViaAPI(String phoneNumber, String message) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("to", phoneNumber);
        requestBody.put("type", "text");

        Map<String, String> text = new HashMap<>();
        text.put("body", message);
        requestBody.put("text", text);

        sendApiRequest(requestBody);
    }

    /**
     * Format phone number
     */
    private String formatPhoneNumber(String phoneNumber) {
        String formatted = phoneNumber.replaceAll("[^0-9+]", "");

        if (!formatted.startsWith("+")) {
            if (formatted.startsWith("0")) {
                formatted = formatted.substring(1);
            }
            formatted = "+91" + formatted; // Default to India
        }

        return formatted;
    }
}