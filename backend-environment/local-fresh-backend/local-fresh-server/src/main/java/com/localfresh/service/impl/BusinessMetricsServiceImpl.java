package com.localfresh.service.impl;

import com.localfresh.service.BusinessMetricsService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class BusinessMetricsServiceImpl implements BusinessMetricsService {

    public static final String PAYMENT_CALLBACK_TOTAL = "localfresh.payment.callback.total";
    public static final String PAYMENT_RECONCILIATION_TOTAL = "localfresh.payment.reconciliation.total";
    public static final String PAYMENT_RECONCILIATION_PENDING_CANDIDATES =
            "localfresh.payment.reconciliation.pending.candidates";
    public static final String ORDER_CANCELLATION_TOTAL = "localfresh.order.cancellation.total";
    public static final String GROUP_BUY_TRANSITION_TOTAL = "localfresh.group_buy.transition.total";

    private static final String TAG_PROVIDER = "provider";
    private static final String TAG_RESULT = "result";

    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<String, AtomicInteger> reconciliationCandidateGauges = new ConcurrentHashMap<>();

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
    public void recordPaymentReconciliationCandidates(String provider, int candidates) {
        String normalizedProvider = normalize(provider);
        AtomicInteger gauge = reconciliationCandidateGauges.computeIfAbsent(normalizedProvider, key -> {
            AtomicInteger value = new AtomicInteger();
            Gauge.builder(PAYMENT_RECONCILIATION_PENDING_CANDIDATES, value, AtomicInteger::get)
                    .description("Latest pending payment reconciliation candidates scanned by provider")
                    .tag(TAG_PROVIDER, key)
                    .register(meterRegistry);
            return value;
        });
        gauge.set(Math.max(candidates, 0));
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
