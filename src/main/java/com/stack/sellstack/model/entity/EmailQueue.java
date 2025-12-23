package com.stack.sellstack.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "email_queue")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "seller_id")
    private String sellerId;

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "template_key")
    private String templateKey;

    @Column(name = "to_email", nullable = false)
    private String toEmail;

    @Column(name = "to_name")
    private String toName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cc_emails", columnDefinition = "jsonb")
    private String[] ccEmails;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "bcc_emails", columnDefinition = "jsonb")
    private String[] bccEmails;

    @Column(name = "subject", nullable = false, length = 500)
    private String subject;

    @Column(name = "html_content", columnDefinition = "TEXT")
    private String htmlContent;

    @Column(name = "plain_text_content", columnDefinition = "TEXT")
    private String plainTextContent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attachments", columnDefinition = "jsonb")
    private Map<String, Object> attachments;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "variables", columnDefinition = "jsonb")
    private Map<String, Object> variables;

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 5;

    @Column(name = "send_after")
    @Temporal(TemporalType.TIMESTAMP)
    private Date sendAfter;

    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 3;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "provider_message_id", length = 500)
    private String providerMessageId;

    @Column(name = "provider_response", columnDefinition = "TEXT")
    private String providerResponse;

    @Column(name = "sent_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date sentAt;

    @Column(name = "delivered_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date deliveredAt;

    @Column(name = "opened_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date openedAt;

    @Column(name = "clicked_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date clickedAt;

    @Column(name = "bounce_reason", columnDefinition = "TEXT")
    private String bounceReason;

    @Column(name = "bounce_type", length = 50)
    private String bounceType;

    @Column(name = "open_count")
    @Builder.Default
    private Integer openCount = 0;

    @Column(name = "click_count")
    @Builder.Default
    private Integer clickCount = 0;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}