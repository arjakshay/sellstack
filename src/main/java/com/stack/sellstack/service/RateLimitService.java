package com.stack.sellstack.service;

import com.stack.sellstack.exception.RateLimitException;
import com.stack.sellstack.model.entity.RateLimit;
import com.stack.sellstack.repository.RateLimitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final RateLimitRepository rateLimitRepository;

    @Transactional
    public void checkRateLimit(String key, String action, int maxAttempts, int windowSeconds) {
        Instant windowStart = Instant.now().minusSeconds(windowSeconds);

        // Count attempts in the time window
        int attemptCount = rateLimitRepository.countAttemptsInWindow(key, action, windowStart);

        if (attemptCount >= maxAttempts) {
            log.warn("Rate limit exceeded: key={}, action={}, attempts={}, max={}",
                    key, action, attemptCount, maxAttempts);
            throw new RateLimitException(
                    String.format("Too many requests. Please try again in %d seconds", windowSeconds)
            );
        }

        // Record this attempt
        RateLimit rateLimit = RateLimit.builder()
                .key(key)
                .action(action)
                .attemptedAt(Instant.now())
                .build();

        rateLimitRepository.save(rateLimit);
    }

    @Transactional
    public int getRemainingAttempts(String key, String action, int maxAttempts, int windowSeconds) {
        Instant windowStart = Instant.now().minusSeconds(windowSeconds);
        int attemptCount = rateLimitRepository.countAttemptsInWindow(key, action, windowStart);
        return Math.max(0, maxAttempts - attemptCount);
    }
}