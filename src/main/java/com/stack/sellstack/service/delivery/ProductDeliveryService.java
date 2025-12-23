package com.stack.sellstack.service.delivery;

import com.stack.sellstack.exception.DeliveryException;
import com.stack.sellstack.model.dto.request.EmailQueueRequest;
import com.stack.sellstack.model.dto.request.ProductDeliveryRequest;
import com.stack.sellstack.model.dto.request.WhatsAppQueueRequest;
import com.stack.sellstack.model.dto.response.ProductDeliveryResponse;
import com.stack.sellstack.model.entity.Order;
import com.stack.sellstack.model.entity.OrderItem;
import com.stack.sellstack.model.entity.Product;
import com.stack.sellstack.repository.OrderRepository;
import com.stack.sellstack.service.notification.EmailService;
import com.stack.sellstack.service.notification.WhatsAppService;
import com.stack.sellstack.service.storage.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductDeliveryService {

    private final OrderRepository orderRepository;
    private final EmailService emailService;
    private final WhatsAppService whatsAppService;
    private final S3Service s3Service;

    @Value("${app.delivery.base-url}")
    private String deliveryBaseUrl;

    @Value("${app.delivery.expiry-hours:72}")
    private int deliveryLinkExpiryHours;

    /**
     * Deliver digital products to customer
     */
    @Transactional
    public ProductDeliveryResponse deliverProducts(ProductDeliveryRequest request) {
        Order order = orderRepository.findByIdWithItems(request.getOrderId())
                .orElseThrow(() -> new DeliveryException("Order not found"));

        // Verify seller owns this order
        if (!order.getSeller().getId().equals(request.getSellerId())) {
            throw new DeliveryException("Unauthorized access to order");
        }

        // Check if order is paid
        if (!"PAID".equals(order.getPaymentStatus())) {
            throw new DeliveryException("Order is not paid");
        }

        // Check if already delivered
        if (order.getDeliveredAt() != null) {
            throw new DeliveryException("Order already delivered");
        }

        // Generate secure delivery links
        List<Map<String, Object>> deliveryLinks = generateDeliveryLinks(order);

        // Send delivery notifications
        boolean emailSent = sendDeliveryEmail(order, deliveryLinks);
        boolean whatsappSent = sendDeliveryWhatsApp(order, deliveryLinks);

        // Update order status
        order.setDeliveredAt(LocalDateTime.now());
        order.setDeliveryMethod(request.getDeliveryMethod());
        order.setDeliveryLinks(deliveryLinks);
        orderRepository.save(order);

        log.info("Products delivered for order: {}. Email: {}, WhatsApp: {}",
                order.getId(), emailSent, whatsappSent);

        return ProductDeliveryResponse.builder()
                .orderId(order.getId().toString())
                .deliveredAt(order.getDeliveredAt())
                .emailSent(emailSent)
                .whatsappSent(whatsappSent)
                .deliveryLinks(deliveryLinks)
                .message("Products delivered successfully")
                .build();
    }

    /**
     * Generate secure, expirable delivery links
     */
    private List<Map<String, Object>> generateDeliveryLinks(Order order) {
        List<Map<String, Object>> links = new ArrayList<>();

        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();

            // Check if product is digital (has file URL)
            if (product.getFileUrl() == null || product.getFileUrl().isEmpty()) {
                continue; // Skip non-digital products
            }

            // Extract file key from URL (assuming S3 URL format)
            String fileKey = extractFileKeyFromUrl(product.getFileUrl());
            if (fileKey == null) {
                log.warn("Could not extract file key from URL: {}", product.getFileUrl());
                continue;
            }

            // Generate secure download link
            String downloadUrl = s3Service.generatePresignedUrl(
                    fileKey,
                    deliveryLinkExpiryHours
            );

            // Generate view link (if preview URL exists)
            String viewUrl = null;
            if (product.getPreviewUrl() != null && !product.getPreviewUrl().isEmpty()) {
                viewUrl = deliveryBaseUrl + "/view/" +
                        generateSecureToken(product.getId().toString(), order.getId().toString());
            }

            Map<String, Object> link = new HashMap<>();
            link.put("productId", product.getId().toString());
            link.put("productName", product.getTitle());
            link.put("downloadUrl", downloadUrl);
            link.put("viewUrl", viewUrl);
            link.put("expiresAt", LocalDateTime.now().plusHours(deliveryLinkExpiryHours));
            link.put("downloadLimit", product.getMaxDownloads() != null ? product.getMaxDownloads() : 3);
            link.put("expiryDays", product.getDownloadExpiryDays() != null ? product.getDownloadExpiryDays() : 30);
            link.put("allowRefunds", product.getAllowRefunds() != null ? product.getAllowRefunds() : true);
            link.put("refundDays", product.getRefundDays() != null ? product.getRefundDays() : 7);

            links.add(link);
        }

        return links;
    }

    /**
     * Extract S3 file key from URL
     */
    private String extractFileKeyFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return null;
        }

        try {
            // Handle different URL formats
            if (fileUrl.contains("amazonaws.com/")) {
                // S3 URL format: https://bucket.s3.region.amazonaws.com/key
                return fileUrl.substring(fileUrl.indexOf("amazonaws.com/") + 14);
            } else if (fileUrl.contains("cloudfront.net/")) {
                // CloudFront URL format: https://distribution.cloudfront.net/key
                return fileUrl.substring(fileUrl.indexOf("cloudfront.net/") + 15);
            } else if (fileUrl.contains("/")) {
                // Simple path format: /folder/filename.ext
                String[] parts = fileUrl.split("/");
                if (parts.length > 1) {
                    // Join all parts except the first empty string
                    return String.join("/", Arrays.copyOfRange(parts, 1, parts.length));
                }
            }

            return fileUrl;
        } catch (Exception e) {
            log.error("Failed to extract file key from URL: {}", fileUrl, e);
            return fileUrl; // Return URL as-is
        }
    }

    /**
     * Send delivery email
     */
    private boolean sendDeliveryEmail(Order order, List<Map<String, Object>> deliveryLinks) {
        try {
            // Prepare template variables
            Map<String, Object> variables = new HashMap<>();
            variables.put("customerName", order.getCustomerName());
            variables.put("orderId", order.getId().toString());
            variables.put("orderNumber", order.getOrderNumber());
            variables.put("orderDate", order.getCreatedAt());
            variables.put("totalAmount", order.getTotalAmount());
            variables.put("deliveryLinks", deliveryLinks);
            variables.put("expiryHours", deliveryLinkExpiryHours);
            variables.put("productCount", deliveryLinks.size());

            // Prepare email request
            EmailQueueRequest emailRequest = EmailQueueRequest.builder()
                    .sellerId(order.getSeller().getId().toString())
                    .orderId(order.getId().toString())
                    .templateKey("PRODUCT_DELIVERY")
                    .to(order.getCustomerEmail())
                    .toName(order.getCustomerName())
                    .variables(variables)
                    .priority(1) // High priority
                    .build();

            // Queue email
            emailService.queueEmail(emailRequest);

            return true;

        } catch (Exception e) {
            log.error("Failed to send delivery email for order: {}", order.getId(), e);
            return false;
        }
    }

    /**
     * Send delivery WhatsApp
     */
    private boolean sendDeliveryWhatsApp(Order order, List<Map<String, Object>> deliveryLinks) {
        try {
            // Prepare message variables
            Map<String, Object> variables = new HashMap<>();
            variables.put("1", order.getCustomerName()); // Template variable 1
            variables.put("2", order.getOrderNumber()); // Template variable 2 (order number)
            variables.put("3", String.valueOf(deliveryLinks.size())); // Product count

            // Prepare delivery links text (truncate if too long)
            StringBuilder linksText = new StringBuilder();
            int linkCount = 0;
            for (Map<String, Object> link : deliveryLinks) {
                if (linkCount < 3) { // Limit to 3 links in WhatsApp
                    linksText.append("• ")
                            .append(link.get("productName"))
                            .append("\nDownload: ")
                            .append(shortenUrl(link.get("downloadUrl").toString()))
                            .append("\n");
                    linkCount++;
                }
            }

            if (deliveryLinks.size() > 3) {
                linksText.append("... and ").append(deliveryLinks.size() - 3).append(" more products");
            }

            variables.put("4", linksText.toString());

            // Prepare WhatsApp request
            WhatsAppQueueRequest whatsAppRequest = WhatsAppQueueRequest.builder()
                    .sellerId(order.getSeller().getId().toString())
                    .orderId(order.getId().toString())
                    .templateKey("PRODUCT_DELIVERY")
                    .toPhone(formatPhoneNumber(order.getCustomerPhone()))
                    .toName(order.getCustomerName())
                    .variables(variables)
                    .priority(1)
                    .build();

            // Queue WhatsApp message
            whatsAppService.queueMessage(whatsAppRequest);

            return true;

        } catch (Exception e) {
            log.error("Failed to send delivery WhatsApp for order: {}", order.getId(), e);
            return false;
        }
    }

    /**
     * Format phone number for WhatsApp
     */
    private String formatPhoneNumber(String phone) {
        if (phone == null || phone.isEmpty()) {
            return phone;
        }

        // Remove spaces, dashes, parentheses
        String cleaned = phone.replaceAll("[\\s\\-\\(\\)]", "");

        // Ensure starts with +
        if (!cleaned.startsWith("+")) {
            // Assume Indian number if no country code
            if (cleaned.startsWith("0")) {
                cleaned = "+91" + cleaned.substring(1);
            } else if (cleaned.length() == 10) {
                cleaned = "+91" + cleaned;
            }
        }

        return cleaned;
    }

    private String shortenUrl(String url) {
        // Simple URL shortening (you can integrate with a URL shortener service)
        if (url.length() > 40) {
            return url.substring(0, 40) + "...";
        }
        return url;
    }

    /**
     * Generate secure token for view links
     */
    private String generateSecureToken(String productId, String orderId) {
        String token = productId + ":" + orderId + ":" + System.currentTimeMillis();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(token.getBytes());
    }

    /**
     * Validate delivery token
     */
    public boolean validateDeliveryToken(String token, String productId, String orderId) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token));
            String[] parts = decoded.split(":");

            if (parts.length != 3) return false;

            // Check if token matches
            if (!parts[0].equals(productId) || !parts[1].equals(orderId)) {
                return false;
            }

            // Check if token is expired (24 hours)
            long tokenTime = Long.parseLong(parts[2]);
            long currentTime = System.currentTimeMillis();
            long twentyFourHours = 24 * 60 * 60 * 1000;

            return (currentTime - tokenTime) < twentyFourHours;

        } catch (Exception e) {
            log.error("Failed to validate delivery token", e);
            return false;
        }
    }

    /**
     * Resend delivery links
     */
    @Transactional
    public ProductDeliveryResponse resendDeliveryLinks(String orderId, String sellerId,
                                                       String channel) {
        Order order = orderRepository.findById(java.util.UUID.fromString(orderId))
                .orElseThrow(() -> new DeliveryException("Order not found"));

        // Verify seller owns this order
        if (!order.getSeller().getId().equals(sellerId)) {
            throw new DeliveryException("Unauthorized access to order");
        }

        // Get existing delivery links
        List<Map<String, Object>> deliveryLinks = order.getDeliveryLinks();

        if (deliveryLinks == null || deliveryLinks.isEmpty()) {
            throw new DeliveryException("No delivery links found");
        }

        boolean success = false;

        if ("EMAIL".equalsIgnoreCase(channel) || "BOTH".equalsIgnoreCase(channel)) {
            success = sendDeliveryEmail(order, deliveryLinks) || success;
        }

        if ("WHATSAPP".equalsIgnoreCase(channel) || "BOTH".equalsIgnoreCase(channel)) {
            success = sendDeliveryWhatsApp(order, deliveryLinks) || success;
        }

        if (!success) {
            throw new DeliveryException("Failed to resend delivery links");
        }

        return ProductDeliveryResponse.builder()
                .orderId(order.getId().toString())
                .deliveredAt(order.getDeliveredAt())
                .emailSent("EMAIL".equalsIgnoreCase(channel) || "BOTH".equalsIgnoreCase(channel))
                .whatsappSent("WHATSAPP".equalsIgnoreCase(channel) || "BOTH".equalsIgnoreCase(channel))
                .deliveryLinks(deliveryLinks)
                .message("Delivery links resent successfully")
                .build();
    }

    /**
     * Get delivery analytics for seller
     */
    @Transactional(readOnly = true)
    public com.stack.sellstack.model.dto.response.DeliveryAnalyticsResponse getDeliveryAnalytics(String sellerId,
                                                                                                 com.stack.sellstack.model.dto.request.DateRangeRequest dateRange) {
        // Get orders delivered in date range
        List<Order> deliveredOrders = orderRepository
                .findDeliveredOrdersBySellerAndDateRange(
                        java.util.UUID.fromString(sellerId),
                        dateRange.getStartDate(),
                        dateRange.getEndDate());

        // Calculate metrics
        long totalOrders = deliveredOrders.size();
        long emailDeliveries = deliveredOrders.stream()
                .filter(order -> order.getDeliveryMethod() != null &&
                        order.getDeliveryMethod().contains("EMAIL"))
                .count();
        long whatsappDeliveries = deliveredOrders.stream()
                .filter(order -> order.getDeliveryMethod() != null &&
                        order.getDeliveryMethod().contains("WHATSAPP"))
                .count();

        // Calculate average delivery time (from payment to delivery)
        double avgDeliveryTimeHours = deliveredOrders.stream()
                .filter(order -> order.getPaidAt() != null && order.getDeliveredAt() != null)
                .mapToLong(order ->
                        java.time.Duration.between(order.getPaidAt(), order.getDeliveredAt())
                                .toHours())
                .average()
                .orElse(0.0);

        return com.stack.sellstack.model.dto.response.DeliveryAnalyticsResponse.builder()
                .totalOrders(totalOrders)
                .emailDeliveries(emailDeliveries)
                .whatsappDeliveries(whatsappDeliveries)
                .avgDeliveryTimeHours(avgDeliveryTimeHours)
                .periodStart(dateRange.getStartDate())
                .periodEnd(dateRange.getEndDate())
                .build();
    }
}