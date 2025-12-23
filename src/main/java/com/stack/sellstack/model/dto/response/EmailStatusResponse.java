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
public class EmailStatusResponse {

    private String emailId;
    private String status;
    private String provider;
    private String messageId;
    private Date sentAt;
    private Date deliveredAt;
    private Date openedAt;
    private Integer openedCount;
    private Integer clickedCount;
    private String bounceReason;
    private String bounceType;
}