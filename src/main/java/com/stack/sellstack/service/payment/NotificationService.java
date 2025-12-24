package com.stack.sellstack.service.payment;

import com.stack.sellstack.model.entity.Payment;
import com.stack.sellstack.model.entity.Product;
import com.stack.sellstack.model.entity.Refund;
import com.stack.sellstack.model.entity.Seller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final EmailService emailService;
    private final WhatsAppService whatsAppService;

    public void sendPaymentSuccessNotification(Payment payment) {
        try {
            Seller buyer = payment.getBuyer();
            Product product = payment.getProduct();

            // Format amount
            DecimalFormat df = new DecimalFormat("₹ #,##0.00");
            String formattedAmount = df.format(payment.getAmount());

            // Send email to buyer
            String buyerEmailSubject = "Payment Successful - " + product.getTitle();
            String buyerEmailBody = buildPaymentSuccessEmail(
                    buyer.getDisplayName(),
                    product.getTitle(),
                    formattedAmount,
                    payment.getReceiptNumber(),
                    payment.getPaymentMethod()
            );

            emailService.sendEmail(buyer.getEmail(), buyerEmailSubject, buyerEmailBody);

            // Send WhatsApp to buyer if phone available
            if (buyer.getPhone() != null) {
                String whatsAppMessage = buildPaymentSuccessWhatsApp(
                        product.getTitle(),
                        formattedAmount,
                        payment.getReceiptNumber()
                );

                whatsAppService.sendMessage(buyer.getPhone(), whatsAppMessage);
            }

            // Send notification to seller
            Seller seller = payment.getSeller();
            BigDecimal sellerShare = calculateSellerShare(payment.getAmount());
            String sellerFormattedShare = df.format(sellerShare);

            String sellerEmailSubject = "New Sale! - " + product.getTitle();
            String sellerEmailBody = buildNewSaleEmail(
                    seller.getDisplayName(),
                    product.getTitle(),
                    formattedAmount,
                    sellerFormattedShare,
                    buyer.getDisplayName() != null ? buyer.getDisplayName() : buyer.getEmail()
            );

            emailService.sendEmail(seller.getEmail(), sellerEmailSubject, sellerEmailBody);

            log.info("Payment success notifications sent for payment: {}", payment.getId());

        } catch (Exception e) {
            log.error("Error sending payment success notifications", e);
            // Don't throw exception - payment shouldn't fail due to notification error
        }
    }

    public void sendProductDeliveryNotification(Payment payment) {
        try {
            Seller buyer = payment.getBuyer();
            Product product = payment.getProduct();

            // Generate download link (you need to implement this)
            String downloadLink = generateDownloadLink(payment);

            String emailSubject = "Your Download is Ready - " + product.getTitle();
            String emailBody = buildDeliveryEmail(
                    buyer.getDisplayName(),
                    product.getTitle(),
                    downloadLink,
                    product.getDownloadExpiryDays()
            );

            emailService.sendEmail(buyer.getEmail(), emailSubject, emailBody);

            log.info("Product delivery notification sent for payment: {}", payment.getId());

        } catch (Exception e) {
            log.error("Error sending product delivery notification", e);
        }
    }

    public void sendRefundNotification(Payment payment, Refund refund) {
        try {
            Seller buyer = payment.getBuyer();
            Product product = payment.getProduct();
            Seller seller = payment.getSeller();

            DecimalFormat df = new DecimalFormat("₹ #,##0.00");
            String formattedAmount = df.format(refund.getAmount());
            String formattedPaymentAmount = df.format(payment.getAmount());

            // Send to buyer
            String buyerEmailSubject = "Refund Processed - " + product.getTitle();
            String buyerEmailBody = buildRefundEmail(
                    buyer.getDisplayName(),
                    product.getTitle(),
                    formattedAmount,
                    formattedPaymentAmount,
                    refund.getReason(),
                    refund.getStatus()
            );

            emailService.sendEmail(buyer.getEmail(), buyerEmailSubject, buyerEmailBody);

            // Send to seller
            String sellerEmailSubject = "Refund Issued - " + product.getTitle();
            String sellerEmailBody = buildRefundIssuedEmail(
                    seller.getDisplayName(),
                    product.getTitle(),
                    formattedAmount,
                    buyer.getDisplayName() != null ? buyer.getDisplayName() : buyer.getEmail(),
                    refund.getReason()
            );

            emailService.sendEmail(seller.getEmail(), sellerEmailSubject, sellerEmailBody);

            log.info("Refund notifications sent for payment: {}", payment.getId());

        } catch (Exception e) {
            log.error("Error sending refund notifications", e);
        }
    }

    // Helper methods
    private String buildPaymentSuccessEmail(String buyerName, String productTitle,
                                            String amount, String receipt, String method) {
        return """
            <html>
            <body>
                <h2>Payment Successful!</h2>
                <p>Dear %s,</p>
                <p>Your payment has been successfully processed.</p>
                <div style="background: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0;">
                    <p><strong>Product:</strong> %s</p>
                    <p><strong>Amount:</strong> %s</p>
                    <p><strong>Receipt No:</strong> %s</p>
                    <p><strong>Payment Method:</strong> %s</p>
                </div>
                <p>You will receive your download link shortly.</p>
                <p>Thank you for your purchase!</p>
                <p>Best regards,<br>The SellStack Team</p>
            </body>
            </html>
            """.formatted(buyerName, productTitle, amount, receipt, method);
    }

    private String buildPaymentSuccessWhatsApp(String productTitle, String amount, String receipt) {
        return """
            ✅ Payment Successful!
            
            Product: %s
            Amount: %s
            Receipt: %s
            
            Your download link will be sent shortly.
            """.formatted(productTitle, amount, receipt);
    }

    private BigDecimal calculateSellerShare(BigDecimal amount) {
        // 10% platform fee
        BigDecimal platformFee = amount.multiply(new BigDecimal("0.10"));
        return amount.subtract(platformFee);
    }

    private String buildNewSaleEmail(String sellerName, String productTitle,
                                     String amount, String sellerShare, String buyerInfo) {
        return """
            <html>
            <body>
                <h2>🎉 New Sale!</h2>
                <p>Dear %s,</p>
                <p>Congratulations! You have made a new sale.</p>
                <div style="background: #e8f5e9; padding: 15px; border-radius: 5px; margin: 15px 0;">
                    <p><strong>Product:</strong> %s</p>
                    <p><strong>Sale Amount:</strong> %s</p>
                    <p><strong>Your Earnings:</strong> %s (after 10%% platform fee)</p>
                    <p><strong>Buyer:</strong> %s</p>
                </div>
                <p>This amount will be added to your available balance after processing.</p>
                <p>Keep creating amazing products!</p>
                <p>Best regards,<br>The SellStack Team</p>
            </body>
            </html>
            """.formatted(sellerName, productTitle, amount, sellerShare, buyerInfo);
    }

    private String generateDownloadLink(Payment payment) {
        // Implement your download link generation logic
        // This should include a secure token and expiry
        return "https://sellstack.com/download/" + payment.getId() + "?token=" + generateDownloadToken(payment);
    }

    private String generateDownloadToken(Payment payment) {
        // Implement secure token generation
        return java.util.UUID.randomUUID().toString();
    }

    private String buildDeliveryEmail(String buyerName, String productTitle,
                                      String downloadLink, Integer expiryDays) {
        return """
            <html>
            <body>
                <h2>Your Download is Ready! 📥</h2>
                <p>Dear %s,</p>
                <p>Thank you for purchasing <strong>%s</strong>.</p>
                <p>Your download link is ready:</p>
                <div style="text-align: center; margin: 25px 0;">
                    <a href="%s" style="background: #4361ee; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px; font-weight: bold;">
                        Download Now
                    </a>
                </div>
                <p><strong>Important:</strong></p>
                <ul>
                    <li>This link will expire in %d days</li>
                    <li>You can download the file up to 3 times</li>
                    <li>Keep this email for future reference</li>
                </ul>
                <p>If you have any issues, please contact our support team.</p>
                <p>Enjoy your purchase!</p>
                <p>Best regards,<br>The SellStack Team</p>
            </body>
            </html>
            """.formatted(buyerName, productTitle, downloadLink, expiryDays);
    }

    private String buildRefundEmail(String buyerName, String productTitle,
                                    String refundAmount, String paymentAmount,
                                    String reason, String status) {
        return """
            <html>
            <body>
                <h2>Refund Processed</h2>
                <p>Dear %s,</p>
                <p>Your refund for <strong>%s</strong> has been processed.</p>
                <div style="background: #fff3cd; padding: 15px; border-radius: 5px; margin: 15px 0;">
                    <p><strong>Original Amount:</strong> %s</p>
                    <p><strong>Refunded Amount:</strong> %s</p>
                    <p><strong>Status:</strong> %s</p>
                    <p><strong>Reason:</strong> %s</p>
                </div>
                <p>The refund will be credited to your original payment method within 5-7 business days.</p>
                <p>If you have any questions, please contact our support team.</p>
                <p>Best regards,<br>The SellStack Team</p>
            </body>
            </html>
            """.formatted(buyerName, productTitle, paymentAmount, refundAmount, status, reason);
    }

    private String buildRefundIssuedEmail(String sellerName, String productTitle,
                                          String refundAmount, String buyerInfo, String reason) {
        return """
            <html>
            <body>
                <h2>Refund Issued</h2>
                <p>Dear %s,</p>
                <p>A refund has been issued for your product <strong>%s</strong>.</p>
                <div style="background: #f8d7da; padding: 15px; border-radius: 5px; margin: 15px 0;">
                    <p><strong>Product:</strong> %s</p>
                    <p><strong>Refund Amount:</strong> %s</p>
                    <p><strong>Buyer:</strong> %s</p>
                    <p><strong>Reason:</strong> %s</p>
                </div>
                <p>This amount has been deducted from your available balance.</p>
                <p>If you believe this refund was issued in error, please contact our support team.</p>
                <p>Best regards,<br>The SellStack Team</p>
            </body>
            </html>
            """.formatted(sellerName, productTitle, productTitle, refundAmount, buyerInfo, reason);
    }

    public void sendPaymentFailedNotification(Payment payment) {
        try {
            Seller buyer = payment.getBuyer();
            Product product = payment.getProduct();

            String emailSubject = "Payment Failed - " + product.getTitle();
            String emailBody = buildPaymentFailedEmail(
                    buyer.getDisplayName(),
                    product.getTitle(),
                    payment.getAmount(),
                    payment.getRazorpayOrderId()
            );

            emailService.sendEmail(buyer.getEmail(), emailSubject, emailBody);

            log.info("Payment failed notification sent for payment: {}", payment.getId());

        } catch (Exception e) {
            log.error("Error sending payment failed notification", e);
        }
    }

    private String buildPaymentFailedEmail(String buyerName, String productTitle,
                                           BigDecimal amount, String orderId) {
        return """
        <html>
        <body>
            <h2>Payment Failed</h2>
            <p>Dear %s,</p>
            <p>Your payment for <strong>%s</strong> has failed.</p>
            <div style="background: #f8d7da; padding: 15px; border-radius: 5px; margin: 15px 0;">
                <p><strong>Product:</strong> %s</p>
                <p><strong>Amount:</strong> ₹%s</p>
                <p><strong>Order ID:</strong> %s</p>
            </div>
            <p>Please try again or contact support if the issue persists.</p>
            <p>Best regards,<br>The SellStack Team</p>
        </body>
        </html>
        """.formatted(buyerName, productTitle, productTitle, amount, orderId);
    }

    public void sendBalanceCreditedNotification(UUID sellerId, BigDecimal amount, UUID transactionId) {
        log.info("Balance credited notification sent to seller: {}, amount: {}, transaction: {}",
                sellerId, amount, transactionId);
        // Implement actual notification (email, SMS, push)
    }

    public void sendPayoutRequestedNotification(UUID sellerId, BigDecimal amount, UUID payoutId) {
        log.info("Payout requested notification sent to seller: {}, amount: {}, payout: {}",
                sellerId, amount, payoutId);
        // Implement actual notification
    }

    public void sendBalanceDebitedNotification(UUID sellerId, BigDecimal amount, UUID transactionId) {
        log.info("Balance debited notification sent to seller: {}, amount: {}, transaction: {}",
                sellerId, amount, transactionId);
    }
}