package com.stack.sellstack.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties(prefix = "razorpay")
@Data
@Slf4j
public class RazorpayConfig {

    private String keyId;
    private String keySecret;
    private String webhookSecret;
    private String mode = "TEST";

    // 🛠️ These inner configuration objects were missing
    private CheckoutConfig checkout = new CheckoutConfig();
    private MethodsConfig methods = new MethodsConfig();
    private UpiConfig upi = new UpiConfig();
    private boolean netbankingEnabled = true;
    private boolean cardEnabled = true;
    private boolean walletEnabled = true;
    private boolean upiEnabled = true;
    private int checkoutTimeout = 900;
    private int upiTimeout = 300;
    private int upiMaxAmount = 10000000;

    @Data
    public static class CheckoutConfig {
        private String name = "SellStack";
        private String description = "Digital Product Purchase";
        private boolean prefillContact = true;
        private boolean prefillEmail = true;
        private String themeColor = "#4361ee";
        private int timeout = 900; // seconds
    }

    @Data
    public static class MethodsConfig {
        private boolean card = true;
        private boolean netbanking = true;
        private boolean upi = true;
        private boolean wallet = true;
        private boolean emi = false;
    }

    @Data
    public static class UpiConfig {
        private boolean intentEnabled = true;
        private boolean collectEnabled = true;
        private int maxAmount = 10000000; // 1,00,000 INR in paise
        private int timeout = 300; // seconds
    }

    @PostConstruct
    public void validate() {
        if (keyId == null || keySecret == null) {
            throw new IllegalStateException("Razorpay credentials are not configured properly");
        }

        if (!"TEST".equals(mode) && !"LIVE".equals(mode)) {
            throw new IllegalStateException("Razorpay mode must be either TEST or LIVE");
        }

        // ✅ FIXED: Call getters on the instance, not the class
        log.info("Razorpay configured in {} mode. UPI enabled: {}, Netbanking enabled: {}",
                mode, getMethods().isUpi(), getMethods().isNetbanking());
    }
}