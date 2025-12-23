package com.stack.sellstack.exception;

import lombok.Getter;

@Getter
public class PaymentException extends RuntimeException {

    private final String errorCode;
    private final String paymentId;
    private final String razorpayErrorCode;

    public PaymentException(String message) {
        super(message);
        this.errorCode = "PAYMENT_ERROR";
        this.paymentId = null;
        this.razorpayErrorCode = null;
    }

    public PaymentException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.paymentId = null;
        this.razorpayErrorCode = null;
    }

    public PaymentException(String message, String errorCode, String paymentId) {
        super(message);
        this.errorCode = errorCode;
        this.paymentId = paymentId;
        this.razorpayErrorCode = null;
    }

    public PaymentException(String message, String errorCode, String paymentId, String razorpayErrorCode) {
        super(message);
        this.errorCode = errorCode;
        this.paymentId = paymentId;
        this.razorpayErrorCode = razorpayErrorCode;
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "PAYMENT_ERROR";
        this.paymentId = null;
        this.razorpayErrorCode = null;
    }
}