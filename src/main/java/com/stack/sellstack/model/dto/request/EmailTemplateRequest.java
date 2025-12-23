package com.stack.sellstack.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplateRequest {

    private UUID id;

    @NotBlank(message = "Template key is required")
    private String templateKey;

    @NotBlank(message = "Template name is required")
    private String templateName;

    @NotBlank(message = "Subject template is required")
    private String subjectTemplate;

    @NotBlank(message = "HTML template is required")
    private String htmlTemplate;

    private String plainTextTemplate;

    private Map<String, Object> variables; // JSON string

    private String category;

    private Integer priority;

    private String[] tags;

    @NotBlank(message = "Created by is required")
    private String createdBy;

    private String updatedBy;
}
