package com.stack.sellstack.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "email.aws.ses")
public class EmailProperties {

    private String fromEmail;
    private String fromName;
    private String sourceArn;
    private String configurationSet;
    private String replyTo;
    private String region = "ap-south-1";
}