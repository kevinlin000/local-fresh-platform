package com.localfresh.task;

import com.localfresh.service.PaymentReconciliationService;
import com.localfresh.service.payment.PaymentReconciliationSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "localfresh.payment.reconciliation.enabled", havingValue = "true")
@Slf4j
public class PaymentReconciliationTask {

    @Autowired
    private PaymentReconciliationService paymentReconciliationService;

    @Value("${localfresh.payment.reconciliation.batch-size:20}")
    private int batchSize;

    @Scheduled(cron = "${localfresh.payment.reconciliation.cron:0 */5 * * * ?}")
    public void reconcilePendingPaymentRequests() {
        PaymentReconciliationSummary summary = paymentReconciliationService.reconcilePendingRequests(batchSize);
        log.info("付款對帳排程完成: {}", summary);
    }
}
