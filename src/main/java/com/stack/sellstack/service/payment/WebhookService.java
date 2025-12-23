package com.stack.sellstack.service.payment;

import com.stack.sellstack.model.entity.*;
import com.stack.sellstack.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookService {

    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final SellerRepository sellerRepository;
    private final PaymentTransactionRepository paymentTransactionRepository; // FIXED: Changed from TransactionRepository
    private final SellerBalanceRepository sellerBalanceRepository; // FIXED: Added for balance management
    private final NotificationService notificationService;

    @Transactional
    public void processWebhookEvent(String payload) {
        try {
            JSONObject event = new JSONObject(payload);
            String eventType = event.getString("event");

            // Extract payment entity from payload
            JSONObject paymentEntity = event.getJSONObject("payload")
                    .getJSONObject("payment")
                    .getJSONObject("entity");

            log.info("Processing webhook event: {}", eventType);

            switch (eventType) {
                case "payment.authorized":
                    handlePaymentAuthorized(paymentEntity);
                    break;
                case "payment.captured":
                    handlePaymentCaptured(paymentEntity);
                    break;
                case "payment.failed":
                    handlePaymentFailed(paymentEntity);
                    break;
                case "refund.created":
                    handleRefundCreated(paymentEntity);
                    break;
                case "refund.processed":
                    handleRefundProcessed(paymentEntity);
                    break;
                case "dispute.created":
                    handleDisputeCreated(paymentEntity);
                    break;
                case "dispute.resolved":
                    handleDisputeResolved(paymentEntity);
                    break;
                default:
                    log.warn("Unhandled webhook event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing webhook event", e);
            throw new RuntimeException("Failed to process webhook: " + e.getMessage(), e);
        }
    }

    private void handlePaymentAuthorized(JSONObject payment) {
        String paymentId = payment.getString("id");
        String orderId = payment.getString("order_id");

        log.info("Payment authorized - Payment ID: {}, Order ID: {}", paymentId, orderId);

        // Find and update payment status
        paymentRepository.findByRazorpayPaymentId(paymentId)
                .ifPresent(paymentEntity -> {
                    paymentEntity.setStatus("AUTHORIZED");
                    paymentRepository.save(paymentEntity);

                    log.info("Payment status updated to AUTHORIZED for payment: {}", paymentId);
                });
    }

    private void handlePaymentCaptured(JSONObject payment) {
        String paymentId = payment.getString("id");
        String orderId = payment.getString("order_id");
        Integer amount = payment.getInt("amount");
        String status = payment.getString("status");
        String paymentMethod = payment.optString("method", "unknown");

        Payment paymentEntity = paymentRepository.findByRazorpayPaymentId(paymentId)
                .orElseThrow(() -> new IllegalStateException("Payment not found: " + paymentId));

        // Convert amount from paise to rupees
        BigDecimal amountInRupees = new BigDecimal(amount).divide(new BigDecimal(100));

        // Update payment status
        paymentEntity.setStatus(status);
        paymentEntity.setCapturedAt(Instant.now());
        paymentEntity.setPaymentMethod(paymentMethod);

        // If UPI payment, store UPI ID
        if ("upi".equals(paymentMethod) && payment.has("vpa")) {
            paymentEntity.setUpiId(payment.getString("vpa"));
        }

        paymentRepository.save(paymentEntity);

        // Create transaction record
        Map<String, Object> transactionNotes = new HashMap<>();
        transactionNotes.put("razorpayPaymentId", paymentId);
        transactionNotes.put("orderId", orderId);
        transactionNotes.put("paymentMethod", paymentMethod);

        PaymentTransaction transaction = PaymentTransaction.builder()
                .payment(paymentEntity)
                .seller(paymentEntity.getSeller())
                .amount(paymentEntity.getAmount())
                .type("CREDIT")
                .status("COMPLETED")
                .description("Payment captured for order: " + orderId)
                .notes(transactionNotes)
                .completedAt(Instant.now())
                .build();

        paymentTransactionRepository.save(transaction);

        // Update product sales count
        Product product = paymentEntity.getProduct();
        product.setSalesCount(product.getSalesCount() + 1); // FIXED: Use correct field name
        productRepository.save(product);

        // Update seller balance using SellerBalance entity
        Seller seller = product.getSeller();
        BigDecimal sellerShare = calculateSellerShare(paymentEntity.getAmount());

        // Update seller's available balance
        SellerBalance sellerBalance = sellerBalanceRepository.findBySellerId(seller.getId())
                .orElseGet(() -> SellerBalance.builder()
                        .seller(seller)
                        .availableBalance(BigDecimal.ZERO)
                        .pendingBalance(BigDecimal.ZERO)
                        .totalEarnings(BigDecimal.ZERO)
                        .build());

        sellerBalance.setAvailableBalance(
                sellerBalance.getAvailableBalance().add(sellerShare)
        );
        sellerBalance.setTotalEarnings(
                sellerBalance.getTotalEarnings().add(paymentEntity.getAmount())
        );
        sellerBalanceRepository.save(sellerBalance);

        // Also update seller entity
        seller.setAvailableBalance(seller.getAvailableBalance().add(sellerShare));
        seller.setTotalEarnings(seller.getTotalEarnings().add(paymentEntity.getAmount()));
        seller.setTotalSales(seller.getTotalSales() + 1);
        sellerRepository.save(seller);

        // Send notifications
        notificationService.sendPaymentSuccessNotification(paymentEntity);
        notificationService.sendProductDeliveryNotification(paymentEntity);

        log.info("Payment captured: {}, Amount: {}, Seller Share: {}",
                paymentId, amountInRupees, sellerShare);
    }

    private void handlePaymentFailed(JSONObject payment) {
        String paymentId = payment.getString("id");
        String orderId = payment.getString("order_id");
        String status = payment.getString("status");

        log.info("Payment failed - Payment ID: {}, Order ID: {}, Status: {}",
                paymentId, orderId, status);

        paymentRepository.findByRazorpayPaymentId(paymentId)
                .ifPresent(paymentEntity -> {
                    paymentEntity.setStatus("FAILED");
                    paymentRepository.save(paymentEntity);

                    // Send failure notification
                    notificationService.sendPaymentFailedNotification(paymentEntity);

                    log.info("Payment status updated to FAILED for payment: {}", paymentId);
                });
    }

    private void handleRefundCreated(JSONObject refund) {
        String refundId = refund.getString("id");
        String paymentId = refund.getString("payment_id");
        Integer amount = refund.getInt("amount");
        String status = refund.getString("status");

        log.info("Refund created - Refund ID: {}, Payment ID: {}, Amount: {}, Status: {}",
                refundId, paymentId, amount, status);

        // Update payment status if needed
        paymentRepository.findByRazorpayPaymentId(paymentId)
                .ifPresent(paymentEntity -> {
                    // You might want to create a Refund entity here
                    log.info("Refund created for payment: {}", paymentId);
                });
    }

    private void handleRefundProcessed(JSONObject refund) {
        String refundId = refund.getString("id");
        String paymentId = refund.getString("payment_id");
        Integer amount = refund.getInt("amount");
        String status = refund.getString("status");

        log.info("Refund processed - Refund ID: {}, Payment ID: {}, Amount: {}, Status: {}",
                refundId, paymentId, amount, status);

        // Find payment and update status
        paymentRepository.findByRazorpayPaymentId(paymentId)
                .ifPresent(paymentEntity -> {
                    BigDecimal refundAmount = new BigDecimal(amount).divide(new BigDecimal(100));

                    // Create refund transaction
                    Map<String, Object> transactionNotes = new HashMap<>();
                    transactionNotes.put("razorpayRefundId", refundId);
                    transactionNotes.put("reason", refund.optString("reason", "Unknown"));

                    PaymentTransaction transaction = PaymentTransaction.builder()
                            .payment(paymentEntity)
                            .seller(paymentEntity.getSeller())
                            .amount(refundAmount.negate()) // Negative amount for refund
                            .type("DEBIT")
                            .status("COMPLETED")
                            .description("Refund processed: " + refundId)
                            .notes(transactionNotes)
                            .completedAt(Instant.now())
                            .build();

                    paymentTransactionRepository.save(transaction);

                    // Update seller balance
                    Seller seller = paymentEntity.getSeller();
                    SellerBalance sellerBalance = sellerBalanceRepository.findBySellerId(seller.getId())
                            .orElseThrow(() -> new IllegalStateException("Seller balance not found"));

                    sellerBalance.setAvailableBalance(
                            sellerBalance.getAvailableBalance().subtract(refundAmount)
                    );
                    sellerBalanceRepository.save(sellerBalance);

                    // Update seller entity
                    seller.setAvailableBalance(seller.getAvailableBalance().subtract(refundAmount));
                    sellerRepository.save(seller);

                    log.info("Refund processed and balance updated for payment: {}", paymentId);
                });
    }

    private void handleDisputeCreated(JSONObject dispute) {
        String disputeId = dispute.getString("id");
        String paymentId = dispute.getString("payment_id");

        log.info("Dispute created - Dispute ID: {}, Payment ID: {}", disputeId, paymentId);

        // Handle dispute creation
        // You might want to create a Dispute entity and update payment status
    }

    private void handleDisputeResolved(JSONObject dispute) {
        String disputeId = dispute.getString("id");
        String paymentId = dispute.getString("payment_id");
        String status = dispute.getString("status");

        log.info("Dispute resolved - Dispute ID: {}, Payment ID: {}, Status: {}",
                disputeId, paymentId, status);

        // Handle dispute resolution
        // Update dispute status and potentially process refunds
    }

    private BigDecimal calculateSellerShare(BigDecimal amount) {
        // Apply platform fee (e.g., 10%)
        BigDecimal platformFee = amount.multiply(new BigDecimal("0.10"));
        return amount.subtract(platformFee);
    }
}