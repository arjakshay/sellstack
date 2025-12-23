package com.stack.sellstack.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplateResponse {

    private UUID id;
    private String templateKey;
    private String templateName;
    private String subjectTemplate;
    private String htmlTemplate;
    private String plainTextTemplate;
    private Map<String, Object> variables;
    private String category;
    private Integer priority;
    private String[] tags;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}