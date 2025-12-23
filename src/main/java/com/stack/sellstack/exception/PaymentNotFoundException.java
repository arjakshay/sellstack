package com.stack.sellstack.exception;


public class PaymentNotFoundException extends PaymentException {
    public PaymentNotFoundException(String paymentId) {
        super("Payment not found with ID: " + paymentId, "PAYMENT_NOT_FOUND", paymentId);
    }

    public PaymentNotFoundException(String message, String paymentId) {
        super(message, "PAYMENT_NOT_FOUND", paymentId);
    }
}