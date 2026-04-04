package com.payment_service.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSessionResponse {
    private String sessionUrl;
    private String sessionId;
    private Long bookingId;
    private Long amount;
    private String status;
}
