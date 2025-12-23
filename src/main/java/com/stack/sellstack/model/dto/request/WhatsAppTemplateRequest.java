package com.stack.sellstack.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppTemplateRequest {

    private String id;

    @NotBlank(message = "Template name is required")
    private String templateName;

    @NotBlank(message = "Template key is required")
    private String templateKey;

    @NotBlank(message = "Language code is required")
    private String languageCode;

    private String whatsappTemplateId;
    private String whatsappNamespace;
    private String category;
    private String headerType;

    @NotBlank(message = "Body template is required")
    private String bodyTemplate;

    private String footerTemplate;
    private Map<String, Object> buttonSchema;
    private Map<String, Object> variableSchema;
    private String status;
    private Boolean isActive;
}