package com.stack.sellstack.exception;

public class RefundException extends PaymentException {

    public RefundException(String message) {
        super(message, "REFUND_ERROR");
    }

    public RefundException(String message, String paymentId) {
        super(message, "REFUND_ERROR", paymentId);
    }

    public RefundException(String message, String paymentId, String refundId) {
        super(message, "REFUND_ERROR", paymentId);
    }
}