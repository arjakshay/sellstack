package com.stack.sellstack.service.monitoring;

import java.util.Map;

public interface AlertService {

    void sendAlert(String alertType, String message);

    void sendAlert(String alertType, String message, Map<String, Object> metadata);

    void sendEmailAlert(String to, String subject, String message);

    void sendSlackAlert(String channel, String message);

    void sendSmsAlert(String phoneNumber, String message);
}