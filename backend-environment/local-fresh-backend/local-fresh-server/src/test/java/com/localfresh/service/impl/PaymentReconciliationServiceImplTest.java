package com.localfresh.service.impl;

import com.github.pagehelper.Page;
import com.localfresh.entity.PaymentEvent;
import com.localfresh.exception.OrderBusinessException;
import com.localfresh.mapper.PaymentEventMapper;
import com.localfresh.service.OrderPaymentService;
import com.localfresh.service.payment.PaymentGateway;
import com.localfresh.service.payment.PaymentQueryResult;
import com.localfresh.service.payment.PaymentQueryStatus;
import com.localfresh.service.payment.PaymentReconciliationSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentReconciliationServiceImplTest {

    @Mock
    private PaymentEventMapper paymentEventMapper;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private OrderPaymentService orderPaymentService;

    @InjectMocks
    private PaymentReconciliationServiceImpl paymentReconciliationService;

    @Test
    void reconcilePendingRequestsShouldApplySucceededProviderQueryResult() {
        when(paymentGateway.provider()).thenReturn("ECPAY");
        Page<PaymentEvent> page = new Page<>();
        page.add(pendingRequest("ORDER-RECON-1"));
        when(paymentEventMapper.pagePendingRequestsWithoutTerminalCallback(any())).thenReturn(page);
        when(paymentGateway.queryPaymentStatus("ORDER-RECON-1")).thenReturn(PaymentQueryResult.builder()
                .provider("ECPAY")
                .orderNumber("ORDER-RECON-1")
                .status(PaymentQueryStatus.SUCCEEDED)
                .providerReference("2026/06/22 18:10:00")
                .providerTradeNo("260622181000001")
                .rawPayload("MerchantTradeNo=ORDER-RECON-1&TradeStatus=1")
                .build());

        PaymentReconciliationSummary summary = paymentReconciliationService.reconcilePendingRequests(20);

        assertEquals(1, summary.getCandidates());
        assertEquals(1, summary.getApplied());
        assertEquals(0, summary.getStillPending());
        ArgumentCaptor<com.localfresh.service.payment.PaymentCallbackCommand> commandCaptor =
                ArgumentCaptor.forClass(com.localfresh.service.payment.PaymentCallbackCommand.class);
        verify(orderPaymentService).handlePaymentCallback(commandCaptor.capture());
        assertEquals("ECPAY", commandCaptor.getValue().getProvider());
        assertEquals("ORDER-RECON-1", commandCaptor.getValue().getOrderNumber());
        assertEquals("260622181000001", commandCaptor.getValue().getProviderTradeNo());
    }

    @Test
    void reconcilePendingRequestsShouldKeepPendingProviderQueryResultForLater() {
        when(paymentGateway.provider()).thenReturn("ECPAY");
        Page<PaymentEvent> page = new Page<>();
        page.add(pendingRequest("ORDER-RECON-2"));
        when(paymentEventMapper.pagePendingRequestsWithoutTerminalCallback(any())).thenReturn(page);
        when(paymentGateway.queryPaymentStatus("ORDER-RECON-2")).thenReturn(PaymentQueryResult.builder()
                .provider("ECPAY")
                .orderNumber("ORDER-RECON-2")
                .status(PaymentQueryStatus.PENDING)
                .rawPayload("MerchantTradeNo=ORDER-RECON-2&TradeStatus=0")
                .build());

        PaymentReconciliationSummary summary = paymentReconciliationService.reconcilePendingRequests(20);

        assertEquals(1, summary.getCandidates());
        assertEquals(0, summary.getApplied());
        assertEquals(1, summary.getStillPending());
        verify(orderPaymentService, never()).handlePaymentCallback(any());
    }

    @Test
    void reconcilePendingRequestsShouldCountRejectedProviderResultWithoutStoppingBatch() {
        when(paymentGateway.provider()).thenReturn("ECPAY");
        Page<PaymentEvent> page = new Page<>();
        page.add(pendingRequest("ORDER-RECON-3"));
        when(paymentEventMapper.pagePendingRequestsWithoutTerminalCallback(any())).thenReturn(page);
        when(paymentGateway.queryPaymentStatus("ORDER-RECON-3")).thenReturn(PaymentQueryResult.builder()
                .provider("ECPAY")
                .orderNumber("ORDER-RECON-3")
                .status(PaymentQueryStatus.FAILED)
                .providerReference("Payment failed")
                .rawPayload("MerchantTradeNo=ORDER-RECON-3&TradeStatus=10200095")
                .build());
        org.mockito.Mockito.doThrow(new OrderBusinessException("付款回呼未成功"))
                .when(orderPaymentService).handlePaymentCallback(any());

        PaymentReconciliationSummary summary = paymentReconciliationService.reconcilePendingRequests(20);

        assertEquals(1, summary.getCandidates());
        assertEquals(1, summary.getRejected());
        assertEquals(0, summary.getQueryErrors());
        verify(orderPaymentService).handlePaymentCallback(any());
    }

    private PaymentEvent pendingRequest(String orderNumber) {
        return PaymentEvent.builder()
                .id(10L)
                .orderId(20L)
                .orderNumber(orderNumber)
                .provider("ECPAY")
                .eventType(PaymentEvent.EVENT_REQUEST_CREATED)
                .providerReference("ECPAY:REQUEST:" + orderNumber)
                .idempotencyKey("ECPAY:REQUEST_CREATED:" + orderNumber + ":ECPAY:REQUEST:" + orderNumber)
                .amount(new BigDecimal("280.00"))
                .result(PaymentEvent.RESULT_PENDING)
                .createdAt(LocalDateTime.of(2026, 6, 22, 18, 0))
                .build();
    }
}
