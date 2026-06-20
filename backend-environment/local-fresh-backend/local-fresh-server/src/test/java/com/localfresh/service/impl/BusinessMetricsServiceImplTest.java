package com.localfresh.service.impl;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BusinessMetricsServiceImplTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final BusinessMetricsServiceImpl businessMetricsService = new BusinessMetricsServiceImpl(meterRegistry);

    @Test
    void recordPaymentCallbackShouldTagProviderAndResult() {
        businessMetricsService.recordPaymentCallback("ECPAY", "SUCCEEDED");
        businessMetricsService.recordPaymentCallback("ECPAY", "SUCCEEDED");
        businessMetricsService.recordPaymentCallback(null, "REJECTED");

        assertEquals(2.0, meterRegistry.counter(BusinessMetricsServiceImpl.PAYMENT_CALLBACK_TOTAL,
                "provider", "ecpay", "result", "succeeded").count());
        assertEquals(1.0, meterRegistry.counter(BusinessMetricsServiceImpl.PAYMENT_CALLBACK_TOTAL,
                "provider", "unknown", "result", "rejected").count());
    }

    @Test
    void recordOrderCancellationShouldTagResult() {
        businessMetricsService.recordOrderCancellation("applied");
        businessMetricsService.recordOrderCancellation("duplicate");

        assertEquals(1.0, meterRegistry.counter(BusinessMetricsServiceImpl.ORDER_CANCELLATION_TOTAL,
                "result", "applied").count());
        assertEquals(1.0, meterRegistry.counter(BusinessMetricsServiceImpl.ORDER_CANCELLATION_TOTAL,
                "result", "duplicate").count());
    }

    @Test
    void recordGroupBuyTransitionShouldTagResult() {
        businessMetricsService.recordGroupBuyTransition("completed");

        assertEquals(1.0, meterRegistry.counter(BusinessMetricsServiceImpl.GROUP_BUY_TRANSITION_TOTAL,
                "result", "completed").count());
    }
}
