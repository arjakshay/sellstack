package com.stack.sellstack.service.payment;

import com.razorpay.*;
import com.stack.sellstack.config.RazorpayConfig;
import com.stack.sellstack.exception.PaymentException;
import com.stack.sellstack.model.dto.request.PaymentOrderRequest;
import com.stack.sellstack.model.dto.request.RefundRequest;
import com.stack.sellstack.model.dto.request.UPIPaymentRequest;
import com.stack.sellstack.model.dto.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class RazorpayService {

    private final RazorpayConfig razorpayConfig;
    private final RestTemplate restTemplate;
    private RazorpayClient razorpayClient;

    private RazorpayClient getRazorpayClient() {
        if (razorpayClient == null) {
            String keyId = razorpayConfig.getKeyId();
            String keySecret = razorpayConfig.getKeySecret();

            if (keyId == null || keySecret == null) {
                throw new IllegalStateException("Razorpay credentials not configured");
            }

            try {
                razorpayClient = new RazorpayClient(keyId, keySecret);
            } catch (RazorpayException e) {
                log.error("Failed to initialize Razorpay client", e);
                throw new PaymentException("Payment service initialization failed");
            }
        }
        return razorpayClient;
    }

    /**
     * Method 1: Using REST API directly (recommended - more reliable)
     */
    public PaymentOrderResponse createPaymentOrder(PaymentOrderRequest request) {
        validatePaymentRequest(request);

        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", request.getAmount()); // Amount in paise
            orderRequest.put("currency", request.getCurrency());
            orderRequest.put("receipt", request.getReceipt());
            orderRequest.put("payment_capture", request.isAutoCapture() ? 1 : 0);
            orderRequest.put("partial_payment", false);

            // Add notes for internal tracking
            JSONObject notes = new JSONObject();
            notes.put("product_id", request.getProductId());
            notes.put("seller_id", request.getSellerId());
            notes.put("buyer_id", request.getBuyerId());
            notes.put("order_type", "PRODUCT_PURCHASE");
            orderRequest.put("notes", notes);

            // Set order timeout
            orderRequest.put("timeout", razorpayConfig.getCheckoutTimeout());

            log.info("Creating Razorpay order: {}", orderRequest.toString());

            // Make REST API call
            String url = "https://api.razorpay.com/v1/orders";
            String auth = razorpayConfig.getKeyId() + ":" + razorpayConfig.getKeySecret();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + encodedAuth);

            HttpEntity<String> entity = new HttpEntity<>(orderRequest.toString(), headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            JSONObject razorpayOrder = new JSONObject(response.getBody());

            // Convert JSON to proper types
            Map<String, Object> notesMap = new HashMap<>();
            if (razorpayOrder.has("notes")) {
                try {
                    JSONObject notesJson = razorpayOrder.getJSONObject("notes");
                    notesMap = notesJson.toMap();
                } catch (JSONException e) {
                    log.warn("Failed to parse notes from order response");
                }
            }

            return PaymentOrderResponse.builder()
                    .orderId(razorpayOrder.getString("id"))
                    .amount(razorpayOrder.getInt("amount"))
                    .currency(razorpayOrder.getString("currency"))
                    .receipt(razorpayOrder.getString("receipt"))
                    .status(razorpayOrder.getString("status"))
                    .createdAt(Instant.ofEpochSecond(razorpayOrder.getLong("created_at")))
                    .razorpayKeyId(razorpayConfig.getKeyId())
                    .notes(notesMap)
                    .build();

        } catch (Exception e) {
            log.error("Failed to create Razorpay order for receipt: {}", request.getReceipt(), e);
            throw new PaymentException("Failed to create payment order: " + e.getMessage());
        }
    }

    /**
     * Method 2: Using SDK (if you want to use the SDK)
     */
    public PaymentOrderResponse createPaymentOrderWithSDK(PaymentOrderRequest request) {
        validatePaymentRequest(request);

        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", request.getAmount());
            orderRequest.put("currency", request.getCurrency());
            orderRequest.put("receipt", request.getReceipt());
            orderRequest.put("payment_capture", request.isAutoCapture() ? 1 : 0);

            // Add notes
            JSONObject notes = new JSONObject();
            notes.put("product_id", request.getProductId());
            notes.put("seller_id", request.getSellerId());
            notes.put("buyer_id", request.getBuyerId());
            notes.put("order_type", "PRODUCT_PURCHASE");
            orderRequest.put("notes", notes);

            log.info("Creating Razorpay order with SDK: {}", orderRequest.toString());

            // SDK returns JSONObject, not com.razorpay.Order
            JSONObject razorpayOrder = getRazorpayClient().orders.create(orderRequest).toJson();

            // Convert JSON to proper types
            Map<String, Object> notesMap = new HashMap<>();
            if (razorpayOrder.has("notes")) {
                try {
                    JSONObject notesJson = razorpayOrder.getJSONObject("notes");
                    notesMap = notesJson.toMap();
                } catch (JSONException e) {
                    log.warn("Failed to parse notes from order response");
                }
            }

            return PaymentOrderResponse.builder()
                    .orderId(razorpayOrder.getString("id"))
                    .amount(razorpayOrder.getInt("amount"))
                    .currency(razorpayOrder.getString("currency"))
                    .receipt(razorpayOrder.getString("receipt"))
                    .status(razorpayOrder.getString("status"))
                    .createdAt(Instant.ofEpochSecond(razorpayOrder.getLong("created_at")))
                    .razorpayKeyId(razorpayConfig.getKeyId())
                    .notes(notesMap)
                    .build();

        } catch (Exception e) {
            log.error("Failed to create Razorpay order with SDK for receipt: {}", request.getReceipt(), e);
            throw new PaymentException("Failed to create payment order: " + e.getMessage());
        }
    }

    public boolean verifyPaymentSignature(String paymentId, String orderId, String signature) {
        if (paymentId == null || orderId == null || signature == null) {
            return false;
        }

        try {
            String payload = orderId + "|" + paymentId;
            String generatedSignature = calculateHMAC(payload, razorpayConfig.getKeySecret());

            boolean isValid = generatedSignature.equals(signature);

            if (!isValid) {
                log.warn("Invalid payment signature for paymentId: {}", paymentId);
            }

            return isValid;

        } catch (Exception e) {
            log.error("Error verifying payment signature", e);
            return false;
        }
    }

    public PaymentCaptureResponse capturePayment(String paymentId, Integer amount) {
        try {
            JSONObject captureRequest = new JSONObject();
            captureRequest.put("amount", amount);

            // SDK returns JSONObject
            JSONObject payment = getRazorpayClient().payments.capture(paymentId, captureRequest).toJson();

            return PaymentCaptureResponse.builder()
                    .paymentId(payment.getString("id"))
                    .orderId(payment.getString("order_id"))
                    .amount(new BigDecimal(payment.getInt("amount")))
                    .currency(payment.getString("currency"))
                    .status(payment.getString("status"))
                    .paymentMethod(payment.getString("method"))
                    .capturedAt(Instant.now())
                    .build();

        } catch (RazorpayException | JSONException e) {
            log.error("Failed to capture payment: {}", paymentId, e);
            throw new PaymentException("Payment capture failed: " + e.getMessage());
        }
    }

    /**
     * Create UPI payment link using REST API
     */
    public UPIPaymentResponse createUPIPaymentLink(UPIPaymentRequest request) {
        validateUPIRequest(request);

        try {
            // Convert BigDecimal amount to paise
            int amountInPaise = request.getAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .intValue();

            // Create payment request using REST API
            JSONObject paymentRequest = new JSONObject();
            paymentRequest.put("amount", amountInPaise);
            paymentRequest.put("currency", request.getCurrency());
            paymentRequest.put("description", request.getDescription());

            if (request.getCustomerId() != null) {
                JSONObject customer = new JSONObject();
                customer.put("id", request.getCustomerId());
                paymentRequest.put("customer", customer);
            }

            // Calculate expiry timestamp
            long expireBy = Instant.now().getEpochSecond() + razorpayConfig.getUpiTimeout();
            paymentRequest.put("expire_by", expireBy);

            // Add notes
            JSONObject notes = new JSONObject();
            notes.put("product_id", request.getProductId());
            notes.put("order_id", request.getOrderId());
            paymentRequest.put("notes", notes);

            // Configure UPI method
            JSONObject method = new JSONObject();
            method.put("method", "upi");

            JSONObject upiDetails = new JSONObject();
            if (request.isIntentFlow()) {
                upiDetails.put("flow", "intent");
                if (request.getUpiApp() != null) {
                    upiDetails.put("app", request.getUpiApp());
                }
            } else {
                upiDetails.put("flow", "collect");
                if (request.getUpiId() != null) {
                    upiDetails.put("vpa", request.getUpiId());
                }
            }
            method.put("upi", upiDetails);
            paymentRequest.put("method", method);

            log.info("Creating UPI payment: {}", paymentRequest.toString());

            // Make REST API call to create payment
            String url = "https://api.razorpay.com/v1/payments";
            String auth = razorpayConfig.getKeyId() + ":" + razorpayConfig.getKeySecret();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + encodedAuth);

            HttpEntity<String> entity = new HttpEntity<>(paymentRequest.toString(), headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            JSONObject paymentResponse = new JSONObject(response.getBody());

            // Extract values safely
            String upiId = "";
            String qrCodeUrl = "";
            String intentUrl = "";

            if (paymentResponse.has("vpa")) {
                upiId = paymentResponse.getString("vpa");
            }

            if (paymentResponse.has("acquirer_data")) {
                try {
                    JSONObject acquirerData = paymentResponse.getJSONObject("acquirer_data");
                    if (acquirerData.has("qr_code")) {
                        qrCodeUrl = acquirerData.getString("qr_code");
                    }
                    if (acquirerData.has("intent_url")) {
                        intentUrl = acquirerData.getString("intent_url");
                    }
                } catch (JSONException e) {
                    log.warn("Failed to parse acquirer_data for UPI payment");
                }
            }

            return UPIPaymentResponse.builder()
                    .upiLinkId(paymentResponse.getString("id"))
                    .upiId(upiId)
                    .qrCodeUrl(qrCodeUrl)
                    .intentUrl(intentUrl)
                    .amount(BigDecimal.valueOf(paymentResponse.getInt("amount")))
                    .status(paymentResponse.getString("status"))
                    .expiresAt(Date.from(Instant.ofEpochSecond(expireBy)))
                    .build();

        } catch (Exception e) {
            log.error("Failed to create UPI payment link", e);
            throw new PaymentException("UPI payment creation failed: " + e.getMessage());
        }
    }

    public RefundResponse processRefund(RefundRequest request) {
        validateRefundRequest(request);

        try {
            // Convert BigDecimal amount to paise
            int amountInPaise = request.getAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .intValue();

            JSONObject refundRequest = new JSONObject();
            refundRequest.put("payment_id", request.getPaymentId());
            refundRequest.put("amount", amountInPaise);
            refundRequest.put("speed", request.getSpeed());

            // Use receipt from request or generate one
            String receipt = request.getReceipt() != null ?
                    request.getReceipt() : "REFUND_" + System.currentTimeMillis();
            refundRequest.put("receipt", receipt);

            JSONObject notes = new JSONObject();
            notes.put("reason", request.getReason());
            notes.put("initiated_by", request.getInitiatedBy() != null ?
                    request.getInitiatedBy() : "SYSTEM");
            notes.put("refund_type", request.getRefundType());
            refundRequest.put("notes", notes);

            if (request.getIdempotencyKey() != null) {
                refundRequest.put("idempotency_key", request.getIdempotencyKey());
            }

            // SDK returns JSONObject
            JSONObject razorpayRefund = getRazorpayClient().refunds.create(refundRequest).toJson();

            Instant processedAt = null;
            if (razorpayRefund.has("processed_at") && !razorpayRefund.isNull("processed_at")) {
                processedAt = Instant.ofEpochSecond(razorpayRefund.getLong("processed_at"));
            }

            return RefundResponse.builder()
                    .refundId(razorpayRefund.getString("id"))
                    .paymentId(razorpayRefund.getString("payment_id"))
                    .amount(new BigDecimal(razorpayRefund.getInt("amount")))
                    .currency(razorpayRefund.getString("currency"))
                    .status(razorpayRefund.getString("status"))
                    .speedRequested(razorpayRefund.optString("speed_requested", request.getSpeed()))
                    .speedProcessed(razorpayRefund.optString("speed_processed", null))
                    .receipt(razorpayRefund.optString("receipt", receipt))
                    .createdAt(Instant.ofEpochSecond(razorpayRefund.getLong("created_at")))
                    .processedAt(processedAt)
                    .build();

        } catch (RazorpayException | JSONException e) {
            log.error("Failed to process refund for payment: {}", request.getPaymentId(), e);
            throw new PaymentException("Refund processing failed: " + e.getMessage());
        }
    }

    public boolean verifyWebhookSignature(String payload, String signature) {
        if (payload == null || signature == null) {
            return false;
        }

        try {
            String generatedSignature = calculateHMAC(payload, razorpayConfig.getWebhookSecret());
            return generatedSignature.equals(signature);

        } catch (Exception e) {
            log.error("Error verifying webhook signature", e);
            return false;
        }
    }

    public PaymentDetails fetchPaymentDetails(String paymentId) {
        try {
            // SDK returns JSONObject
            JSONObject payment = getRazorpayClient().payments.fetch(paymentId).toJson();

            // Extract optional fields safely
            Map<String, Object> notesMap = new HashMap<>();
            if (payment.has("notes")) {
                try {
                    JSONObject notesJson = payment.getJSONObject("notes");
                    notesMap = notesJson.toMap();
                } catch (JSONException e) {
                    log.warn("Failed to parse notes for payment {}", paymentId);
                }
            }

            BigDecimal fee = BigDecimal.ZERO;
            BigDecimal tax = BigDecimal.ZERO;

            if (payment.has("fee")) {
                fee = new BigDecimal(payment.getInt("fee"));
            }
            if (payment.has("tax")) {
                tax = new BigDecimal(payment.getInt("tax"));
            }

            return PaymentDetails.builder()
                    .paymentId(payment.getString("id"))
                    .orderId(payment.getString("order_id"))
                    .amount(new BigDecimal(payment.getInt("amount")))
                    .currency(payment.getString("currency"))
                    .status(payment.getString("status"))
                    .paymentMethod(payment.getString("method"))
                    .description(payment.optString("description", ""))
                    .email(payment.optString("email", ""))
                    .contact(payment.optString("contact", ""))
                    .fee(fee)
                    .tax(tax)
                    .createdAt(Instant.ofEpochSecond(payment.getLong("created_at")))
                    .notes(notesMap)
                    .build();

        } catch (RazorpayException | JSONException e) {
            log.error("Failed to fetch payment details: {}", paymentId, e);
            throw new PaymentException("Failed to fetch payment details: " + e.getMessage());
        }
    }

    /**
     * Alternative: Fetch payment details via REST API
     */
    public PaymentDetails fetchPaymentDetailsViaRest(String paymentId) {
        try {
            String url = "https://api.razorpay.com/v1/payments/" + paymentId;
            String auth = razorpayConfig.getKeyId() + ":" + razorpayConfig.getKeySecret();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + encodedAuth);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            JSONObject payment = new JSONObject(response.getBody());

            // Extract optional fields safely
            Map<String, Object> notesMap = new HashMap<>();
            if (payment.has("notes")) {
                try {
                    JSONObject notesJson = payment.getJSONObject("notes");
                    notesMap = notesJson.toMap();
                } catch (JSONException e) {
                    log.warn("Failed to parse notes for payment {}", paymentId);
                }
            }

            BigDecimal fee = BigDecimal.ZERO;
            BigDecimal tax = BigDecimal.ZERO;

            if (payment.has("fee")) {
                fee = new BigDecimal(payment.getInt("fee"));
            }
            if (payment.has("tax")) {
                tax = new BigDecimal(payment.getInt("tax"));
            }

            return PaymentDetails.builder()
                    .paymentId(payment.getString("id"))
                    .orderId(payment.getString("order_id"))
                    .amount(new BigDecimal(payment.getInt("amount")))
                    .currency(payment.getString("currency"))
                    .status(payment.getString("status"))
                    .paymentMethod(payment.getString("method"))
                    .description(payment.optString("description", ""))
                    .email(payment.optString("email", ""))
                    .contact(payment.optString("contact", ""))
                    .fee(fee)
                    .tax(tax)
                    .createdAt(Instant.ofEpochSecond(payment.getLong("created_at")))
                    .notes(notesMap)
                    .build();

        } catch (Exception e) {
            log.error("Failed to fetch payment details via REST: {}", paymentId, e);
            throw new PaymentException("Failed to fetch payment details: " + e.getMessage());
        }
    }

    private String calculateHMAC(String data, String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secretKey);

        byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();

        for (byte b : hash) {
            result.append(String.format("%02x", b));
        }

        return result.toString();
    }

    private void validatePaymentRequest(PaymentOrderRequest request) {
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new PaymentException("Invalid payment amount");
        }

        if (request.getAmount() < 100) {
            throw new PaymentException("Minimum payment amount is 1 INR (100 paise)");
        }

        if (request.getAmount() > 1000000000) {
            throw new PaymentException("Maximum payment amount exceeded");
        }

        if (request.getReceipt() == null || request.getReceipt().length() > 40) {
            throw new PaymentException("Invalid receipt ID");
        }
    }

    private void validateUPIRequest(UPIPaymentRequest request) {
        if (!razorpayConfig.isUpiEnabled()) {
            throw new PaymentException("UPI payments are not enabled");
        }

        // Convert amount to paise for comparison
        BigDecimal amountInPaise = request.getAmount().multiply(BigDecimal.valueOf(100));
        if (amountInPaise.compareTo(BigDecimal.valueOf(razorpayConfig.getUpiMaxAmount())) > 0) {
            throw new PaymentException("UPI payment amount exceeds maximum limit");
        }
    }

    private void validateRefundRequest(RefundRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentException("Invalid refund amount");
        }

        if (request.getPaymentId() == null) {
            throw new PaymentException("Payment ID is required for refund");
        }

        if (request.getReason() == null || request.getReason().length() > 255) {
            throw new PaymentException("Invalid refund reason");
        }
    }
}