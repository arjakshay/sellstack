package com.stack.sellstack.service.payment;

import com.razorpay.RazorpayException;
import com.stack.sellstack.exception.PaymentException;
import com.stack.sellstack.exception.PaymentNotFoundException;
import com.stack.sellstack.exception.PaymentValidationException;
import com.stack.sellstack.exception.RefundException;
import com.stack.sellstack.model.dto.request.*;
import com.stack.sellstack.model.dto.response.*;
import com.stack.sellstack.model.entity.*;
import com.stack.sellstack.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final RazorpayService razorpayService;
    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final SellerBalanceRepository sellerBalanceRepository;
    private final RefundRepository refundRepository;
    private final ProductRepository productRepository;
    private final SellerRepository sellerRepository;
    private final NotificationService notificationService;

    @Transactional
    public PaymentOrderResponse createPaymentOrder(PaymentOrderRequest request) {
        try {
            log.info("Creating payment order for product: {}, buyer: {}",
                    request.getProductId(), request.getBuyerId());

            // Validate product exists
            Product product = productRepository.findById(UUID.fromString(request.getProductId()))
                    .orElseThrow(() -> new PaymentValidationException("Product not found: " + request.getProductId()));

            // Validate buyer exists
            Seller buyer = sellerRepository.findById(UUID.fromString(request.getBuyerId()))
                    .orElseThrow(() -> new PaymentValidationException("Buyer not found: " + request.getBuyerId()));

            // Validate seller exists
            Seller seller = product.getSeller();
            if (seller == null) {
                throw new PaymentValidationException("Seller not found for product");
            }

            // Convert amount to paise for Razorpay
            // FIXED: request.getAmount() returns Integer, not BigDecimal
            Integer amountInPaise = request.getAmount(); // Already in paise

            // Create Razorpay order using the correct DTO
            com.stack.sellstack.model.dto.request.PaymentOrderRequest razorpayRequest =
                    com.stack.sellstack.model.dto.request.PaymentOrderRequest.builder()
                            .amount(amountInPaise) // Already in paise
                            .currency("INR")
                            .receipt(generateReceiptNumber())
                            .productId(product.getId().toString())
                            .sellerId(seller.getId().toString())
                            .buyerId(buyer.getId().toString())
                            .build();

            // Call local RazorpayService method
            com.stack.sellstack.model.dto.response.PaymentOrderResponse razorpayResponse =
                    razorpayService.createPaymentOrder(razorpayRequest);

            // Save payment record
            // FIXED: Convert paise to rupees for entity
            BigDecimal amountInRupees = BigDecimal.valueOf(amountInPaise)
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);

            Map<String, Object> notesMap = new HashMap<>();
            // FIXED: Add optional fields if they exist
            if (request.getDescription() != null) {
                notesMap.put("description", request.getDescription());
            }
            if (request.getNotes() != null) {
                notesMap.put("notes", request.getNotes());
            }

            Payment payment = Payment.builder()
                    .razorpayOrderId(razorpayResponse.getOrderId())
                    .receiptNumber(razorpayResponse.getReceipt())
                    .product(product)
                    .seller(seller)
                    .buyer(buyer)
                    .amount(amountInRupees) // Store in rupees
                    .currency(razorpayResponse.getCurrency())
                    .status(razorpayResponse.getStatus())
                    .notes(notesMap)
                    .build();

            paymentRepository.save(payment);

            log.info("Payment order created: {}", payment.getId());

            return PaymentOrderResponse.builder()
                    .orderId(razorpayResponse.getOrderId())
                    .amount(razorpayResponse.getAmount())
                    .currency(razorpayResponse.getCurrency())
                    .receipt(razorpayResponse.getReceipt())
                    .status(razorpayResponse.getStatus())
                    .razorpayKeyId(razorpayResponse.getRazorpayKeyId())
                    .createdAt(Instant.ofEpochSecond(razorpayResponse.getCreatedAt().getEpochSecond()))
                    .build();

        } catch (Exception e) {
            log.error("Error creating payment order", e);
            throw new PaymentException("Failed to create payment order", e);
        }
    }

    @Transactional
    public PaymentVerificationResponse verifyAndProcessPayment(PaymentVerificationRequest request) {
        try {
            log.info("Verifying payment: {}", request.getRazorpayPaymentId());

            // Verify Razorpay signature
            boolean isValid = razorpayService.verifyPaymentSignature(
                    request.getRazorpayPaymentId(),
                    request.getRazorpayOrderId(),
                    request.getRazorpaySignature()
            );

            if (!isValid) {
                log.warn("Invalid payment signature for payment: {}", request.getRazorpayPaymentId());
                return PaymentVerificationResponse.builder()
                        .success(false)
                        .paymentId(request.getRazorpayPaymentId())
                        .orderId(request.getRazorpayOrderId())
                        .status("FAILED")
                        .message("Invalid payment signature")
                        .timestamp(Instant.now().getEpochSecond())
                        .build();
            }

            // Find payment by order ID
            Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                    .orElseThrow(() -> new PaymentNotFoundException(
                            "Payment not found for order: " + request.getRazorpayOrderId(),
                            request.getRazorpayPaymentId()));

            // Update payment with Razorpay details
            payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
            payment.setRazorpaySignature(request.getRazorpaySignature());

            // Capture payment if auto-capture
            if (payment.getStatus().equals("CREATED") || payment.getStatus().equals("AUTHORIZED")) {
                // Convert amount to paise
                Integer amountInPaise = payment.getAmount()
                        .multiply(BigDecimal.valueOf(100))
                        .intValue();

                com.stack.sellstack.model.dto.response.PaymentCaptureResponse captureResponse =
                        razorpayService.capturePayment(request.getRazorpayPaymentId(), amountInPaise);

                payment.setStatus(captureResponse.getStatus());
                payment.setCapturedAt(captureResponse.getCapturedAt());
                // FIXED: Use getPaymentMethod() not getMethod()
                payment.setPaymentMethod(captureResponse.getPaymentMethod());

                // If UPI payment, store UPI details
                if ("upi".equals(captureResponse.getPaymentMethod())) {
                    // Fetch additional payment details for UPI info
                    com.stack.sellstack.model.dto.response.PaymentDetails paymentDetails =
                            razorpayService.fetchPaymentDetails(request.getRazorpayPaymentId());
                    // UPI ID might be in notes or separate field
                    // You'll need to extract it from paymentDetails
                }

                // Update seller balance
                updateSellerBalance(payment);

                // Create transaction record
                createPaymentTransaction(payment);

                // Send notifications
                notificationService.sendPaymentSuccessNotification(payment);
                notificationService.sendProductDeliveryNotification(payment);

                log.info("Payment captured successfully: {}", payment.getId());
            }

            paymentRepository.save(payment);

            return PaymentVerificationResponse.builder()
                    .success(true)
                    .paymentId(payment.getRazorpayPaymentId())
                    .orderId(payment.getRazorpayOrderId())
                    .status(payment.getStatus())
                    .message("Payment verified and processed successfully")
                    .timestamp(Instant.now().getEpochSecond())
                    .build();

        } catch (PaymentNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error verifying payment", e);
            throw new PaymentException("Failed to verify payment: " + e.getMessage());
        }
    }

    @Transactional
    public RefundResponse initiateRefund(RefundRequest request) {
        try {
            log.info("Initiating refund for payment: {}", request.getPaymentId());

            // Find payment
            Payment payment = paymentRepository.findByRazorpayPaymentId(request.getPaymentId())
                    .orElseThrow(() -> new PaymentNotFoundException(request.getPaymentId()));

            // Validate refund eligibility
            if (!payment.isRefundable()) {
                throw new RefundException("Payment is not eligible for refund", payment.getId().toString());
            }

            // Check if refund amount is valid
            if (request.getAmount().compareTo(payment.getAmount()) > 0) {
                throw new RefundException(
                        "Refund amount cannot exceed payment amount: " + payment.getAmount(),
                        payment.getId().toString()
                );
            }

            // Calculate already refunded amount
            BigDecimal alreadyRefunded = refundRepository.sumProcessedRefundsByPayment(payment.getId());
            BigDecimal remainingAmount = payment.getAmount().subtract(alreadyRefunded);

            if (request.getAmount().compareTo(remainingAmount) > 0) {
                throw new RefundException(
                        "Refund amount exceeds remaining refundable amount: " + remainingAmount,
                        payment.getId().toString()
                );
            }

            // Process refund via Razorpay
            Integer amountInPaise = request.getAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .intValue();

            // FIXED: Use the correct DTO from your package
            com.stack.sellstack.model.dto.request.RefundRequest razorpayRefundRequest =
                    com.stack.sellstack.model.dto.request.RefundRequest.builder()
                            .paymentId(request.getPaymentId())
                            .amount(BigDecimal.valueOf(amountInPaise)) // In paise
                            .reason(request.getReason())
                            .speed(request.getSpeed())
                            .idempotencyKey(request.getIdempotencyKey())
                            .build();

            com.stack.sellstack.model.dto.response.RefundResponse razorpayResponse =
                    razorpayService.processRefund(razorpayRefundRequest);

            // Save refund record
            Refund refund = Refund.builder()
                    .payment(payment)
                    .razorpayRefundId(razorpayResponse.getRefundId())
                    .amount(request.getAmount())
                    .currency(razorpayResponse.getCurrency())
                    .status(razorpayResponse.getStatus())
                    .reason(request.getReason())
                    .speedRequested(request.getSpeed())
                    .speedProcessed(razorpayResponse.getSpeedProcessed())
                    .notes(new HashMap<>())
                    .build();

            // Add notes if available
            if (request.getRefundType() != null) {
                refund.getNotes().put("refundType", request.getRefundType());
            }
            if (request.getNotes() != null) {
                refund.getNotes().put("notes", request.getNotes());
            }

            refundRepository.save(refund);

            // Update payment status if fully refunded
            if (request.getAmount().compareTo(payment.getAmount()) == 0) {
                payment.setStatus("REFUNDED");
                payment.setRefundedAt(Instant.now());
                paymentRepository.save(payment);
            }

            // Create debit transaction for seller
            createRefundTransaction(payment, refund);

            // Update seller balance
            deductFromSellerBalance(payment.getSeller().getId(), request.getAmount());

            // Send notification
            notificationService.sendRefundNotification(payment, refund);

            log.info("Refund initiated: {}", refund.getId());

            return RefundResponse.builder()
                    .refundId(razorpayResponse.getRefundId())
                    .paymentId(razorpayResponse.getPaymentId())
                    .amount(refund.getAmount())
                    .currency(refund.getCurrency())
                    .status(refund.getStatus())
                    .speedRequested(refund.getSpeedRequested())
                    .speedProcessed(razorpayResponse.getSpeedProcessed())
                    .receipt(razorpayResponse.getReceipt())
                    .createdAt(refund.getCreatedAt())
                    .processedAt(refund.getProcessedAt())
                    .build();

        } catch (Exception e) {
            log.error("Error processing refund", e);
            throw new RefundException("Failed to process refund: " + e.getMessage(), request.getPaymentId());
        }
    }

    @Transactional
    public void processWebhookEvent(String payload) {
        try {
            log.info("Processing webhook event");
            // Parse and process Razorpay webhook events
            // Implementation depends on specific webhook structure

        } catch (Exception e) {
            log.error("Error processing webhook", e);
            throw new PaymentException("Failed to process webhook: " + e.getMessage());
        }
    }

    public PaymentDetails getPaymentDetails(String paymentId) {
        try {
            com.stack.sellstack.model.dto.response.PaymentDetails razorpayDetails =
                    razorpayService.fetchPaymentDetails(paymentId);

            Payment payment = paymentRepository.findByRazorpayPaymentId(paymentId)
                    .orElse(null);

            // FIXED: Use getPaymentMethod() not getMethod()
            return PaymentDetails.builder()
                    .paymentId(razorpayDetails.getPaymentId())
                    .orderId(razorpayDetails.getOrderId())
                    .amount(razorpayDetails.getAmount())
                    .currency(razorpayDetails.getCurrency())
                    .status(razorpayDetails.getStatus())
                    .paymentMethod(razorpayDetails.getPaymentMethod())
                    .email(razorpayDetails.getEmail())
                    .contact(razorpayDetails.getContact())
                    .fee(razorpayDetails.getFee())
                    .tax(razorpayDetails.getTax())
                    .createdAt(razorpayDetails.getCreatedAt())
                    .notes(razorpayDetails.getNotes())
                    .build();

        } catch (Exception e) {
            log.error("Error fetching payment details", e);
            throw new PaymentException("Failed to fetch payment details: " + e.getMessage());
        }
    }

    // FIXED: Add this missing class or remove the method
    public OrderPaymentsResponse getOrderPayments(String orderId) {
        try {
            log.info("Getting payments for order: {}", orderId);

            // Find all payments for this order (if multiple payments per order)
            List<Payment> payments = paymentRepository.findAllByRazorpayOrderId(orderId);

            if (payments.isEmpty()) {
                throw new PaymentNotFoundException("No payments found for order: " + orderId);
            }

            // Convert to DTOs
            List<PaymentDetails> paymentDetailsList = payments.stream()
                    .map(payment -> PaymentDetails.builder()
                            .paymentId(payment.getRazorpayPaymentId())
                            .orderId(payment.getRazorpayOrderId())
                            .amount(payment.getAmount())
                            .currency(payment.getCurrency())
                            .status(payment.getStatus())
                            .paymentMethod(payment.getPaymentMethod())
                            .createdAt(payment.getCreatedAt())
                            .notes(payment.getNotes())
                            .build())
                    .collect(Collectors.toList());

            // Calculate totals
            BigDecimal totalAmount = payments.stream()
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal successfulAmount = payments.stream()
                    .filter(p -> p.getStatus().equals("CAPTURED") || p.getStatus().equals("COMPLETED"))
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal pendingAmount = payments.stream()
                    .filter(p -> p.getStatus().equals("CREATED") || p.getStatus().equals("AUTHORIZED"))
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal failedAmount = payments.stream()
                    .filter(p -> p.getStatus().equals("FAILED"))
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal refundedAmount = payments.stream()
                    .filter(p -> p.getStatus().equals("REFUNDED"))
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Create summary
            OrderPaymentsResponse.Summary summary = OrderPaymentsResponse.Summary.builder()
                    .successfulAmount(successfulAmount)
                    .pendingAmount(pendingAmount)
                    .failedAmount(failedAmount)
                    .refundedAmount(refundedAmount)
                    .build();

            return OrderPaymentsResponse.builder()
                    .orderId(orderId)
                    .totalAmount(totalAmount)
                    .totalPayments(payments.size())
                    .payments(paymentDetailsList)
                    .summary(summary)
                    .build();

        } catch (PaymentNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting payments for order: {}", orderId, e);
            throw new PaymentException("Failed to get order payments: " + e.getMessage());
        }
    }

    // Private helper methods
    private String generateReceiptNumber() {
        return "RCPT" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    private void updateSellerBalance(Payment payment) {
        Seller seller = payment.getSeller();
        BigDecimal amount = payment.getAmount();

        // Apply platform fee (e.g., 10%)
        BigDecimal platformFee = amount.multiply(new BigDecimal("0.10"));
        BigDecimal sellerShare = amount.subtract(platformFee);

        // Update seller balance
        SellerBalance balance = sellerBalanceRepository.findBySellerId(seller.getId())
                .orElseGet(() -> SellerBalance.builder()
                        .seller(seller)
                        .availableBalance(BigDecimal.ZERO)
                        .pendingBalance(BigDecimal.ZERO)
                        .totalEarnings(BigDecimal.ZERO)
                        .build());

        balance.setPendingBalance(balance.getPendingBalance().add(sellerShare));
        balance.setTotalEarnings(balance.getTotalEarnings().add(amount));

        sellerBalanceRepository.save(balance);

        // Also update the seller entity
        seller.setAvailableBalance(seller.getAvailableBalance().add(sellerShare));
        seller.setTotalEarnings(seller.getTotalEarnings().add(amount));
        seller.setTotalSales(seller.getTotalSales() + 1);

        sellerRepository.save(seller);
    }

    private void createPaymentTransaction(Payment payment) {
        Map<String, Object> transactionNotes = new HashMap<>();
        transactionNotes.put("productId", payment.getProduct().getId().toString());
        transactionNotes.put("buyerId", payment.getBuyer().getId().toString());

        PaymentTransaction transaction = PaymentTransaction.builder()
                .payment(payment)
                .seller(payment.getSeller())
                .amount(payment.getAmount())
                .type("CREDIT")
                .status("COMPLETED")
                .description("Payment for product: " + payment.getProduct().getTitle())
                .notes(transactionNotes)
                .completedAt(Instant.now())
                .build();

        paymentTransactionRepository.save(transaction);
    }

    private void createRefundTransaction(Payment payment, Refund refund) {
        Map<String, Object> transactionNotes = new HashMap<>();
        transactionNotes.put("refundId", refund.getId().toString());
        transactionNotes.put("reason", refund.getReason());

        PaymentTransaction transaction = PaymentTransaction.builder()
                .payment(payment)
                .seller(payment.getSeller())
                .amount(refund.getAmount().negate())
                .type("DEBIT")
                .status("COMPLETED")
                .description("Refund for payment: " + payment.getId())
                .notes(transactionNotes)
                .completedAt(Instant.now())
                .build();

        paymentTransactionRepository.save(transaction);
    }

    private void deductFromSellerBalance(UUID sellerId, BigDecimal amount) {
        int updated = sellerBalanceRepository.deductFromBalance(sellerId, amount);
        if (updated == 0) {
            throw new PaymentException("Insufficient balance for refund", "INSUFFICIENT_BALANCE");
        }
    }
}