package com.stack.sellstack.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppQueueRequest {

    private String sellerId;
    private String orderId;
    private String templateKey;
    private String whatsAppTemplateId;

    @NotBlank(message = "Phone number is required")
    private String toPhone;

    private String toName;
    private Map<String, Object> variables;
    private List<Map<String, Object>> mediaUrls;

    private Integer priority;
    private Date sendAfter;
}
