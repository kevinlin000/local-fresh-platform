package com.localfresh.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEventVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long orderId;
    private String orderNumber;
    private String provider;
    private String eventType;
    private String providerReference;
    private String providerTradeNo;
    private String idempotencyKey;
    private BigDecimal amount;
    private String result;
    private String rawPayload;
    private LocalDateTime createdAt;
}
