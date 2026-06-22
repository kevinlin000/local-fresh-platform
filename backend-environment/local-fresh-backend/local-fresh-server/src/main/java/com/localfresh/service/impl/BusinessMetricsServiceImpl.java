package com.localfresh.service.impl;

import com.localfresh.service.BusinessMetricsService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class BusinessMetricsServiceImpl implements BusinessMetricsService {

    public static final String PAYMENT_CALLBACK_TOTAL = "localfresh.payment.callback.total";
    public static final String PAYMENT_RECONCILIATION_TOTAL = "localfresh.payment.reconciliation.total";
    public static final String ORDER_CANCELLATION_TOTAL = "localfresh.order.cancellation.total";
    public static final String GROUP_BUY_TRANSITION_TOTAL = "localfresh.group_buy.transition.total";

    private static final String TAG_PROVIDER = "provider";
    private static final String TAG_RESULT = "result";

    private final MeterRegistry meterRegistry;

    public BusinessMetricsServiceImpl(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void recordPaymentCallback(String provider, String result) {
        meterRegistry.counter(PAYMENT_CALLBACK_TOTAL,
                TAG_PROVIDER, normalize(provider),
                TAG_RESULT, normalize(result)).increment();
    }

    @Override
    public void recordPaymentReconciliation(String provider, String result) {
        meterRegistry.counter(PAYMENT_RECONCILIATION_TOTAL,
                TAG_PROVIDER, normalize(provider),
                TAG_RESULT, normalize(result)).increment();
    }

    @Override
    public void recordOrderCancellation(String result) {
        meterRegistry.counter(ORDER_CANCELLATION_TOTAL,
                TAG_RESULT, normalize(result)).increment();
    }

    @Override
    public void recordGroupBuyTransition(String result) {
        meterRegistry.counter(GROUP_BUY_TRANSITION_TOTAL,
                TAG_RESULT, normalize(result)).increment();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim().toLowerCase().replace('-', '_');
    }
}
