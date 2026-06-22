package com.localfresh.service;

public interface BusinessMetricsService {

    void recordPaymentCallback(String provider, String result);

    void recordPaymentReconciliation(String provider, String result);

    void recordOrderCancellation(String result);

    void recordGroupBuyTransition(String result);
}
