package com.stack.sellstack.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDeliveryResponse {

    private String orderId;
    private LocalDateTime deliveredAt;
    private boolean emailSent;
    private boolean whatsappSent;
    private List<Map<String, Object>> deliveryLinks;
    private String message;
}