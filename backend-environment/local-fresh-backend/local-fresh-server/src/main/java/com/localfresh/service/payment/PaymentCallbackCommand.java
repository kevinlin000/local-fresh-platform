package com.localfresh.service.payment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentCallbackCommand {

    private String provider;
    private String orderNumber;
    private String providerReference;
    private String providerTradeNo;
    private String rawPayload;
    private boolean paymentSucceeded;
}
