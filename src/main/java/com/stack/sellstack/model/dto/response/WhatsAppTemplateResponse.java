package com.stack.sellstack.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppTemplateResponse {

    private String id;
    private String templateName;
    private String templateKey;
    private String languageCode;
    private String whatsappTemplateId;
    private String whatsappNamespace;
    private String category;
    private String headerType;
    private String bodyTemplate;
    private String footerTemplate;
    private Map<String, Object> buttonSchema;
    private Map<String, Object> variableSchema;
    private String status;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime approvedAt;
    private String rejectionReason;
}