package com.stack.sellstack.exception;


public class PaymentValidationException extends PaymentException {

    public PaymentValidationException(String message) {
        super(message, "VALIDATION_ERROR");
    }

    public PaymentValidationException(String message, String paymentId) {
        super(message, "VALIDATION_ERROR", paymentId);
    }
}