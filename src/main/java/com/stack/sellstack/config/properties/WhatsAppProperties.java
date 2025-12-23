package com.stack.sellstack.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "whatsapp.meta")
public class WhatsAppProperties {

    private String baseUrl = "https://graph.facebook.com/v17.0/";
    private String phoneNumberId;
    private String accessToken;
    private String webhookVerifyToken;
    private String webhookSecret;
    private String businessAccountId;
    private boolean enabled = true;

    // Rate limiting
    private int rateLimitPerSecond = 1;
    private int rateLimitPerDay = 10;
    private int maxRetries = 3;
}