package com.stack.sellstack.security;

import com.stack.sellstack.exception.RateLimitException;
import com.stack.sellstack.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomRateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String ipAddress = SecurityUtils.getClientIP(request);
        String requestUri = request.getRequestURI();
        String httpMethod = request.getMethod();

        try {
            // Apply rate limiting based on IP
            rateLimitService.checkRateLimit(
                    ipAddress,
                    "IP_" + httpMethod + "_" + requestUri,
                    100,  // Max requests per minute
                    60    // 1 minute window
            );

            // Apply stricter rate limiting for auth endpoints
            if (requestUri.startsWith("/api/v1/auth/")) {
                rateLimitService.checkRateLimit(
                        ipAddress,
                        "IP_AUTH_" + httpMethod + "_" + requestUri,
                        20,   // Max requests per minute for auth
                        60    // 1 minute window
                );
            }

            filterChain.doFilter(request, response);

        } catch (RateLimitException e) {
            log.warn("Rate limit exceeded for IP: {}, URI: {}, Method: {}",
                    ipAddress, requestUri, httpMethod);

            // Use the numeric value (429)
            response.setStatus(429); // HTTP 429 Too Many Requests
            response.setContentType("application/json");
            response.setHeader("Retry-After", "60");
            response.getWriter().write(String.format(
                    "{\"error\": \"Rate limit exceeded\", \"message\": \"%s\", \"retryAfter\": 60}",
                    e.getMessage()
            ));
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip rate limiting for health checks and static resources
        String requestUri = request.getRequestURI();
        return requestUri.startsWith("/actuator/health") ||
                requestUri.startsWith("/swagger-ui/") ||
                requestUri.startsWith("/v3/api-docs/") ||
                requestUri.startsWith("/error");
    }
}