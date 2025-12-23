package com.stack.sellstack.controller.payment;

import com.stack.sellstack.model.dto.request.PaymentOrderRequest;
import com.stack.sellstack.model.dto.request.PaymentVerificationRequest;
import com.stack.sellstack.model.dto.request.RefundRequest;
import com.stack.sellstack.model.dto.request.UPIPaymentRequest;
import com.stack.sellstack.model.dto.response.*;
import com.stack.sellstack.service.payment.PaymentService;
import com.stack.sellstack.service.payment.RazorpayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Payment processing APIs")
public class PaymentController {

    private final PaymentService paymentService;
    private final RazorpayService razorpayService;

    @PostMapping("/orders/create")
    @Operation(summary = "Create payment order for product purchase")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('BUYER') or hasRole('SELLER')")
    public ResponseEntity<ApiResponse<PaymentOrderResponse>> createPaymentOrder(
            @Valid @RequestBody PaymentOrderRequest request,
            HttpServletRequest httpRequest) {

        log.info("Creating payment order for product: {}", request.getProductId());

        PaymentOrderResponse response = paymentService.createPaymentOrder(request);

        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Payment order created successfully"
        ));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify payment completion and signature")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('BUYER') or hasRole('SELLER')")
    public ResponseEntity<ApiResponse<PaymentVerificationResponse>> verifyPayment(
            @Valid @RequestBody PaymentVerificationRequest request) {

        log.info("Verifying payment: {}", request.getRazorpayPaymentId());

        PaymentVerificationResponse response = paymentService.verifyAndProcessPayment(request);

        return ResponseEntity.ok(ApiResponse.success(
                response,
                response.isSuccess() ? "Payment verified successfully" : "Payment verification failed"
        ));
    }

    @PostMapping("/upi/create")
    @Operation(summary = "Create UPI payment link (QR/Intent)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('BUYER') or hasRole('SELLER')")
    public ResponseEntity<ApiResponse<UPIPaymentResponse>> createUPIPayment(
            @Valid @RequestBody UPIPaymentRequest request) {

        log.info("Creating UPI payment for order: {}", request.getOrderId());

        UPIPaymentResponse response = razorpayService.createUPIPaymentLink(request);

        return ResponseEntity.ok(ApiResponse.success(
                response,
                "UPI payment link created successfully"
        ));
    }

    @PostMapping("/refunds/initiate")
    @Operation(summary = "Initiate payment refund")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RefundResponse>> initiateRefund(
            @Valid @RequestBody RefundRequest request) {

        log.info("Initiating refund for payment: {}", request.getPaymentId());

        RefundResponse response = paymentService.initiateRefund(request);

        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Refund initiated successfully"
        ));
    }

    @PostMapping("/webhook")
    @Operation(summary = "Razorpay webhook endpoint")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature,
            HttpServletRequest request) {

        log.debug("Received webhook: {}", payload);

        // Verify webhook signature
        if (!razorpayService.verifyWebhookSignature(payload, signature)) {
            log.error("Invalid webhook signature");
            return ResponseEntity.status(401).body("Invalid signature");
        }

        try {
            paymentService.processWebhookEvent(payload);
            return ResponseEntity.ok("Webhook processed");
        } catch (Exception e) {
            log.error("Error processing webhook", e);
            return ResponseEntity.status(500).body("Error processing webhook");
        }
    }

    @GetMapping("/status/{paymentId}")
    @Operation(summary = "Get payment status")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('BUYER') or hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PaymentDetails>> getPaymentStatus(
            @PathVariable String paymentId) {

        PaymentDetails details = razorpayService.fetchPaymentDetails(paymentId);

        return ResponseEntity.ok(ApiResponse.success(
                details,
                "Payment details retrieved"
        ));
    }

    @GetMapping("/orders/{orderId}/payments")
    @Operation(summary = "Get payments for an order")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderPaymentsResponse>> getOrderPayments(
            @PathVariable String orderId) {

        OrderPaymentsResponse response = paymentService.getOrderPayments(orderId);

        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Order payments retrieved"
        ));
    }
}