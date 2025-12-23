package com.stack.sellstack.model.enums;

public enum SecurityEventType {
    // Registration events
    REGISTRATION_INITIATED,
    REGISTRATION_COMPLETED,

    // Login events
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    LOGIN_BLOCKED,

    // Logout events
    LOGOUT,

    // Token events
    TOKEN_REFRESHED,
    TOKEN_EXPIRED,
    TOKEN_INVALID,

    // Password events
    PASSWORD_RESET_INITIATED,
    PASSWORD_RESET_COMPLETED,
    PASSWORD_CHANGED,

    // OTP events
    OTP_GENERATED,
    OTP_VERIFICATION_SUCCESS,
    OTP_VERIFICATION_FAILED,

    // Session events
    SESSION_CREATED,
    SESSION_REVOKED,
    SESSION_EXPIRED,

    // Security events
    SUSPICIOUS_ACTIVITY,
    BRUTE_FORCE_ATTEMPT,
    ACCOUNT_LOCKED,
    ACCOUNT_UNLOCKED,

    // Profile events
    PROFILE_UPDATED,
    EMAIL_CHANGED,
    PHONE_CHANGED
}