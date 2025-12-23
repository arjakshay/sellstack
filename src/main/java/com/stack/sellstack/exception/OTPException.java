package com.stack.sellstack.exception;

import org.springframework.http.HttpStatus;

public class OTPException extends BusinessException {
    public OTPException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}