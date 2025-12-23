package com.stack.sellstack.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppResponse {

    private String messageId;
    private String providerMessageId;
    private String status;
    private Date sentAt;
    private String provider;
    private String errorMessage;
}