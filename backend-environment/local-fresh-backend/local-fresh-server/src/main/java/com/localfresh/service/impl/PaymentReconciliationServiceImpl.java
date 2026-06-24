package com.localfresh.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.localfresh.dto.PaymentEventPageQueryDTO;
import com.localfresh.entity.PaymentEvent;
import com.localfresh.exception.OrderBusinessException;
import com.localfresh.mapper.PaymentEventMapper;
import com.localfresh.service.BusinessMetricsService;
import com.localfresh.service.OrderPaymentService;
import com.localfresh.service.PaymentReconciliationService;
import com.localfresh.service.payment.PaymentCallbackCommand;
import com.localfresh.service.payment.PaymentGateway;
import com.localfresh.service.payment.PaymentQueryResult;
import com.localfresh.service.payment.PaymentQueryStatus;
import com.localfresh.service.payment.PaymentReconciliationSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PaymentReconciliationServiceImpl implements PaymentReconciliationService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final String RESULT_APPLIED = "APPLIED";
    private static final String RESULT_REJECTED = "REJECTED";
    private static final String RESULT_PENDING = "PENDING";
    private static final String RESULT_UNKNOWN = "UNKNOWN";
    private static final String RESULT_QUERY_ERROR = "QUERY_ERROR";
    private static final String RESULT_UNSUPPORTED = "UNSUPPORTED";

    @Autowired
    private PaymentEventMapper paymentEventMapper;

    @Autowired
    private PaymentGateway paymentGateway;

    @Autowired
    private OrderPaymentService orderPaymentService;

    @Autowired
    private BusinessMetricsService businessMetricsService;

    @Override
    public PaymentReconciliationSummary reconcilePendingRequests(int limit) {
        String provider = paymentGateway.provider();
        PaymentEventPageQueryDTO queryDTO = new PaymentEventPageQueryDTO();
        queryDTO.setPage(1);
        queryDTO.setPageSize(normalizeLimit(limit));
        queryDTO.setProvider(provider);
        PageHelper.startPage(queryDTO.getPage(), queryDTO.getPageSize());
        Page<PaymentEvent> pendingRequests = paymentEventMapper.pagePendingRequestsWithoutTerminalCallback(queryDTO);
        businessMetricsService.recordPaymentReconciliationCandidates(provider, pendingRequests.size());

        PaymentReconciliationCounter counter = new PaymentReconciliationCounter(pendingRequests.size());
        for (PaymentEvent pendingRequest : pendingRequests) {
            reconcileOne(pendingRequest, counter);
        }
        return counter.toSummary();
    }

    private void reconcileOne(PaymentEvent pendingRequest, PaymentReconciliationCounter counter) {
        PaymentQueryResult queryResult;
        String provider = paymentGateway.provider();
        try {
            queryResult = paymentGateway.queryPaymentStatus(pendingRequest.getOrderNumber());
        } catch (UnsupportedOperationException ex) {
            log.info("Payment provider does not support reconciliation query: provider={}, orderNumber={}",
                    provider, pendingRequest.getOrderNumber());
            counter.unsupported++;
            businessMetricsService.recordPaymentReconciliation(provider, RESULT_UNSUPPORTED);
            return;
        } catch (Exception ex) {
            log.warn("Payment reconciliation query failed: provider={}, orderNumber={}, errorType={}, errorMessage={}",
                    provider, pendingRequest.getOrderNumber(), ex.getClass().getSimpleName(), ex.getMessage());
            counter.queryErrors++;
            businessMetricsService.recordPaymentReconciliation(provider, RESULT_QUERY_ERROR);
            return;
        }

        PaymentQueryStatus status = queryResult.getStatus();
        if (PaymentQueryStatus.SUCCEEDED.equals(status)) {
            try {
                orderPaymentService.handlePaymentCallback(toCallbackCommand(queryResult, true));
                counter.applied++;
                businessMetricsService.recordPaymentReconciliation(queryResult.getProvider(), RESULT_APPLIED);
            } catch (Exception ex) {
                log.warn("Payment reconciliation could not apply succeeded provider result: provider={}, orderNumber={}, errorType={}, errorMessage={}",
                        queryResult.getProvider(), queryResult.getOrderNumber(), ex.getClass().getSimpleName(),
                        ex.getMessage());
                counter.queryErrors++;
                businessMetricsService.recordPaymentReconciliation(queryResult.getProvider(), RESULT_QUERY_ERROR);
            }
        } else if (PaymentQueryStatus.FAILED.equals(status)) {
            try {
                orderPaymentService.handlePaymentCallback(toCallbackCommand(queryResult, false));
                counter.rejected++;
                businessMetricsService.recordPaymentReconciliation(queryResult.getProvider(), RESULT_REJECTED);
            } catch (OrderBusinessException ex) {
                log.info("Payment reconciliation recorded rejected payment: provider={}, orderNumber={}",
                        queryResult.getProvider(), queryResult.getOrderNumber());
                counter.rejected++;
                businessMetricsService.recordPaymentReconciliation(queryResult.getProvider(), RESULT_REJECTED);
            } catch (Exception ex) {
                log.warn("Payment reconciliation could not record rejected provider result: provider={}, orderNumber={}, errorType={}, errorMessage={}",
                        queryResult.getProvider(), queryResult.getOrderNumber(), ex.getClass().getSimpleName(),
                        ex.getMessage());
                counter.queryErrors++;
                businessMetricsService.recordPaymentReconciliation(queryResult.getProvider(), RESULT_QUERY_ERROR);
            }
        } else if (PaymentQueryStatus.PENDING.equals(status)) {
            counter.stillPending++;
            businessMetricsService.recordPaymentReconciliation(queryResult.getProvider(), RESULT_PENDING);
        } else {
            counter.unknown++;
            businessMetricsService.recordPaymentReconciliation(queryResult.getProvider(), RESULT_UNKNOWN);
        }
    }

    private PaymentCallbackCommand toCallbackCommand(PaymentQueryResult queryResult, boolean paymentSucceeded) {
        return PaymentCallbackCommand.builder()
                .provider(queryResult.getProvider())
                .orderNumber(queryResult.getOrderNumber())
                .providerReference(queryResult.getProviderReference())
                .providerTradeNo(queryResult.getProviderTradeNo())
                .rawPayload(queryResult.getRawPayload())
                .paymentSucceeded(paymentSucceeded)
                .build();
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private static class PaymentReconciliationCounter {
        private final int candidates;
        private int applied;
        private int rejected;
        private int stillPending;
        private int unknown;
        private int queryErrors;
        private int unsupported;

        private PaymentReconciliationCounter(int candidates) {
            this.candidates = candidates;
        }

        private PaymentReconciliationSummary toSummary() {
            return PaymentReconciliationSummary.builder()
                    .candidates(candidates)
                    .applied(applied)
                    .rejected(rejected)
                    .stillPending(stillPending)
                    .unknown(unknown)
                    .queryErrors(queryErrors)
                    .unsupported(unsupported)
                    .build();
        }
    }
}
