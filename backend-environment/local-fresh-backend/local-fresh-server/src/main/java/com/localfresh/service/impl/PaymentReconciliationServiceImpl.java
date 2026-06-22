package com.localfresh.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.localfresh.dto.PaymentEventPageQueryDTO;
import com.localfresh.entity.PaymentEvent;
import com.localfresh.exception.OrderBusinessException;
import com.localfresh.mapper.PaymentEventMapper;
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

    @Autowired
    private PaymentEventMapper paymentEventMapper;

    @Autowired
    private PaymentGateway paymentGateway;

    @Autowired
    private OrderPaymentService orderPaymentService;

    @Override
    public PaymentReconciliationSummary reconcilePendingRequests(int limit) {
        PaymentEventPageQueryDTO queryDTO = new PaymentEventPageQueryDTO();
        queryDTO.setPage(1);
        queryDTO.setPageSize(normalizeLimit(limit));
        queryDTO.setProvider(paymentGateway.provider());
        PageHelper.startPage(queryDTO.getPage(), queryDTO.getPageSize());
        Page<PaymentEvent> pendingRequests = paymentEventMapper.pagePendingRequestsWithoutTerminalCallback(queryDTO);

        PaymentReconciliationCounter counter = new PaymentReconciliationCounter(pendingRequests.size());
        for (PaymentEvent pendingRequest : pendingRequests) {
            reconcileOne(pendingRequest, counter);
        }
        return counter.toSummary();
    }

    private void reconcileOne(PaymentEvent pendingRequest, PaymentReconciliationCounter counter) {
        PaymentQueryResult queryResult;
        try {
            queryResult = paymentGateway.queryPaymentStatus(pendingRequest.getOrderNumber());
        } catch (UnsupportedOperationException ex) {
            log.info("Payment provider does not support reconciliation query: provider={}, orderNumber={}",
                    paymentGateway.provider(), pendingRequest.getOrderNumber());
            counter.unsupported++;
            return;
        } catch (Exception ex) {
            log.warn("Payment reconciliation query failed: provider={}, orderNumber={}",
                    paymentGateway.provider(), pendingRequest.getOrderNumber(), ex);
            counter.queryErrors++;
            return;
        }

        PaymentQueryStatus status = queryResult.getStatus();
        if (PaymentQueryStatus.SUCCEEDED.equals(status)) {
            try {
                orderPaymentService.handlePaymentCallback(toCallbackCommand(queryResult, true));
                counter.applied++;
            } catch (Exception ex) {
                log.warn("Payment reconciliation could not apply succeeded provider result: provider={}, orderNumber={}",
                        queryResult.getProvider(), queryResult.getOrderNumber(), ex);
                counter.queryErrors++;
            }
        } else if (PaymentQueryStatus.FAILED.equals(status)) {
            try {
                orderPaymentService.handlePaymentCallback(toCallbackCommand(queryResult, false));
                counter.rejected++;
            } catch (OrderBusinessException ex) {
                log.info("Payment reconciliation recorded rejected payment: provider={}, orderNumber={}",
                        queryResult.getProvider(), queryResult.getOrderNumber());
                counter.rejected++;
            } catch (Exception ex) {
                log.warn("Payment reconciliation could not record rejected provider result: provider={}, orderNumber={}",
                        queryResult.getProvider(), queryResult.getOrderNumber(), ex);
                counter.queryErrors++;
            }
        } else if (PaymentQueryStatus.PENDING.equals(status)) {
            counter.stillPending++;
        } else {
            counter.unknown++;
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
