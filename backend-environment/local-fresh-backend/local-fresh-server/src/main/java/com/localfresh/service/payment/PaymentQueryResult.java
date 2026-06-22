package com.localfresh.service.payment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentQueryResult {

    private String provider;
    private String orderNumber;
    private PaymentQueryStatus status;
    private String providerReference;
    private String providerTradeNo;
    private String rawPayload;
}
