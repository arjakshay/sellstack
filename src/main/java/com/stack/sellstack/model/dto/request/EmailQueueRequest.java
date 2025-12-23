package com.stack.sellstack.model.dto.request;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailQueueRequest {

    private String sellerId;
    private String orderId;
    private String templateKey;

    @NotBlank(message = "Recipient email is required")
    private String to;

    private String toName;
    private String[] cc;
    private String[] bcc;

    private String subject;
    private String htmlContent;
    private String plainTextContent;

    private Map<String, Object> attachments;
    private Map<String, Object> variables;

    private Integer priority;
    private Date sendAfter;
}