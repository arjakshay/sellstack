package com.stack.sellstack.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "rate_limits",
        indexes = {
                @Index(name = "idx_rate_limit_key_action", columnList = "key, action"),
                @Index(name = "idx_rate_limit_attempted_at", columnList = "attemptedAt")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "key", nullable = false, length = 255)
    private String key; // Could be IP address, phone number, etc.

    @Column(name = "action", nullable = false, length = 50)
    private String action; // e.g., "LOGIN", "OTP_GENERATION"

    @CreationTimestamp
    @Column(name = "attempted_at", nullable = false, updatable = false)
    private Instant attemptedAt;

    @Column(name = "metadata", length = 500)
    private String metadata; // Additional info like user agent, etc.
}