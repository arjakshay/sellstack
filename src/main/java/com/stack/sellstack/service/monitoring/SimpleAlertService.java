package com.stack.sellstack.service.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class SimpleAlertService implements AlertService {

    @Override
    public void sendAlert(String alertType, String message) {
        log.warn("ALERT [{}]: {}", alertType, message);
        // In production, you would integrate with:
        // - Email services
        // - Slack/Teams webhooks
        // - SMS services
        // - Monitoring tools like Datadog, New Relic
    }

    @Override
    public void sendAlert(String alertType, String message, Map<String, Object> metadata) {
        log.warn("ALERT [{}]: {} - Metadata: {}", alertType, message, metadata);
    }

    @Override
    public void sendEmailAlert(String to, String subject, String message) {
        log.warn("Email Alert to {}: {} - {}", to, subject, message);
        // TODO: Integrate with email service
    }

    @Override
    public void sendSlackAlert(String channel, String message) {
        log.warn("Slack Alert to {}: {}", channel, message);
        // TODO: Integrate with Slack webhook
    }

    @Override
    public void sendSmsAlert(String phoneNumber, String message) {
        log.warn("SMS Alert to {}: {}", phoneNumber, message);
        // TODO: Integrate with SMS service
    }
}