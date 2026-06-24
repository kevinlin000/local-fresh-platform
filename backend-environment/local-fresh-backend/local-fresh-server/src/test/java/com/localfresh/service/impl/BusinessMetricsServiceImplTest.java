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
    void recordPaymentReconciliationShouldTagProviderAndResult() {
        businessMetricsService.recordPaymentReconciliation("ECPAY", "APPLIED");
        businessMetricsService.recordPaymentReconciliation("ECPAY", "QUERY_ERROR");
        businessMetricsService.recordPaymentReconciliation("ECPAY", "QUERY_ERROR");

        assertEquals(1.0, meterRegistry.counter(BusinessMetricsServiceImpl.PAYMENT_RECONCILIATION_TOTAL,
                "provider", "ecpay", "result", "applied").count());
        assertEquals(2.0, meterRegistry.counter(BusinessMetricsServiceImpl.PAYMENT_RECONCILIATION_TOTAL,
                "provider", "ecpay", "result", "query_error").count());
    }

    @Test
    void recordPaymentReconciliationCandidatesShouldExposeLatestCandidateCount() {
        businessMetricsService.recordPaymentReconciliationCandidates("ECPAY", 7);
        businessMetricsService.recordPaymentReconciliationCandidates("ECPAY", 2);
        businessMetricsService.recordPaymentReconciliationCandidates(null, -3);

        assertEquals(2.0, meterRegistry.get(BusinessMetricsServiceImpl.PAYMENT_RECONCILIATION_PENDING_CANDIDATES)
                .tag("provider", "ecpay")
                .gauge()
                .value());
        assertEquals(0.0, meterRegistry.get(BusinessMetricsServiceImpl.PAYMENT_RECONCILIATION_PENDING_CANDIDATES)
                .tag("provider", "unknown")
                .gauge()
                .value());
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
