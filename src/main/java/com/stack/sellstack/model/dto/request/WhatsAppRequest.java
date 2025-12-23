package com.stack.sellstack.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppRequest {

    private String sellerId;

    @NotBlank(message = "Phone number is required")
    private String toPhone;

    private String toName;
    private String templateKey;
    private String message;
    private Map<String, Object> variables;
    private List<Map<String, Object>> mediaUrls;
    private String mediaUrl; // For single media
}