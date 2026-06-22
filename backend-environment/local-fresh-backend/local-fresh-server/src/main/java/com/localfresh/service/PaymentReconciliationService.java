package com.localfresh.service;

import com.localfresh.service.payment.PaymentReconciliationSummary;

public interface PaymentReconciliationService {

    PaymentReconciliationSummary reconcilePendingRequests(int limit);
}
