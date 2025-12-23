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
public class EmailRequest {

    @NotBlank(message = "Recipient email is required")
    private String to;

    private String toName;

    @NotBlank(message = "Subject is required")
    private String subject;

    private String htmlContent;
    private String plainTextContent;

    private String[] cc;
    private String[] bcc;

    private Map<String, Object> attachments;
    private String sellerId;
}