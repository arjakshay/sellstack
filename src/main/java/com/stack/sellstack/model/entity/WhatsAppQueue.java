package com.stack.sellstack.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "whatsapp_queue")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "seller_id")
    private String sellerId;

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "template_key", length = 100)
    private String templateKey;

    @Column(name = "whatsapp_template_id", length = 100)
    private String whatsappTemplateId;

    @Column(name = "to_phone", length = 20, nullable = false)
    private String toPhone;

    @Column(name = "to_name", length = 200)
    private String toName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "variables", columnDefinition = "jsonb")
    private Map<String, Object> variables;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "media_urls", columnDefinition = "jsonb")
    private List<Map<String, Object>> mediaUrls;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "send_after")
    @Temporal(TemporalType.TIMESTAMP)
    private Date sendAfter;

    @Column(name = "max_retries")
    private Integer maxRetries;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "provider_message_id", length = 500)
    private String providerMessageId;

    @Column(name = "provider_conversation_id", length = 500)
    private String providerConversationId;

    @Column(name = "provider_response", columnDefinition = "TEXT")
    private String providerResponse;

    @Column(name = "sent_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date sentAt;

    @Column(name = "delivered_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date deliveredAt;

    @Column(name = "read_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date readAt;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}