package com.stack.sellstack.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class HttpClientConfig {

    @Bean
    public OkHttpClient okHttpClient() {
        ConnectionPool connectionPool = new ConnectionPool(
                10, // maxIdleConnections
                5,  // keepAliveDuration in minutes
                TimeUnit.MINUTES
        );

        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .connectionPool(connectionPool)
                .addInterceptor(chain -> {
                    // Add custom headers for all HTTP requests
                    var request = chain.request().newBuilder()
                            .header("User-Agent", "SellStack/1.0")
                            .header("Accept", "application/json")
                            .header("Accept-Language", "en-US,en;q=0.9")
                            .build();
                    return chain.proceed(request);
                })
                .build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules()  // For Java 8 time support
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}