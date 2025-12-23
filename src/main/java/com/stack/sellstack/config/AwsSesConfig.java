package com.stack.sellstack.config;

import com.stack.sellstack.config.properties.EmailProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

import java.net.URI;

@Configuration
public class AwsSesConfig {

    @Value("${aws.access-key-id:}")
    private String awsAccessKey;

    @Value("${aws.secret-access-key:}")
    private String awsSecretKey;

    @Value("${aws.region:ap-south-1}")
    private String awsRegion;

    @Value("${aws.ses.endpoint:}")
    private String sesEndpoint;

    @Bean
    public SesClient sesClient(EmailProperties emailProperties) {
        // Create credentials
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(
                awsAccessKey != null ? awsAccessKey : "",
                awsSecretKey != null ? awsSecretKey : ""
        );

        // Determine region
        String region = emailProperties.getRegion() != null ?
                emailProperties.getRegion() : awsRegion;

        // Build client
        software.amazon.awssdk.services.ses.SesClientBuilder builder = SesClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .region(Region.of(region));

        // For local testing, you can set a custom endpoint
        if (sesEndpoint != null && !sesEndpoint.trim().isEmpty()) {
            builder.endpointOverride(URI.create(sesEndpoint));
        }

        return builder.build();
    }
}