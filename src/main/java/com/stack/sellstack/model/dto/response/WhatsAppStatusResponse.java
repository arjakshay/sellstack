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
public class WhatsAppStatusResponse {

    private String messageId;
    private String status;
    private String provider;
    private String providerMessageId;
    private Date sentAt;
    private Date deliveredAt;
    private Date readAt;
    private String errorCode;
    private String errorMessage;
}