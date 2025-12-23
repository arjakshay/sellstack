package com.stack.sellstack.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {
    // Add getter explicitly if Lombok @Data doesn't work
    private List<String> allowedOrigins;

}