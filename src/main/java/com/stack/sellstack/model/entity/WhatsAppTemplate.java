package com.stack.sellstack.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "whatsapp_templates")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "template_name", nullable = false)
    private String templateName;

    @Column(name = "template_key", unique = true, nullable = false)
    private String templateKey;

    @Column(name = "language_code", length = 10)
    private String languageCode;

    @Column(name = "whatsapp_template_id", length = 100)
    private String whatsappTemplateId;

    @Column(name = "whatsapp_namespace", length = 100)
    private String whatsappNamespace;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "header_type", length = 50)
    private String headerType;

    @Column(name = "body_template", columnDefinition = "TEXT", nullable = false)
    private String bodyTemplate;

    @Column(name = "footer_template", columnDefinition = "TEXT")
    private String footerTemplate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "button_schema", columnDefinition = "jsonb")
    private Map<String, Object> buttonSchema;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "variable_schema", columnDefinition = "jsonb")
    private Map<String, Object> variableSchema;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "is_active")
    private Boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
}