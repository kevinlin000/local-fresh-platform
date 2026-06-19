package com.localfresh.service.impl;

import com.localfresh.constant.MessageConstant;
import com.localfresh.entity.Orders;
import com.localfresh.entity.PaymentEvent;
import com.localfresh.exception.OrderBusinessException;
import com.localfresh.mapper.OrderMapper;
import com.localfresh.mapper.PaymentEventMapper;
import com.localfresh.service.payment.PaymentGateway;
import com.localfresh.vo.OrderPaymentVO;
import com.localfresh.websocket.WebSocketServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPaymentServiceImplTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private WebSocketServer webSocketServer;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private PaymentEventMapper paymentEventMapper;

    @InjectMocks
    private OrderPaymentServiceImpl orderPaymentService;

    @Test
    void requestPaymentShouldCreatePaymentRequestWithoutMarkingPaid() {
        Orders order = orderWithStatus(1L, Orders.PENDING_PAYMENT);
        order.setNumber("ORDER-001");
        order.setAmount(new java.math.BigDecimal("120.00"));
        OrderPaymentVO expectedVO = OrderPaymentVO.builder()
                .packageStr("ecpay-form:ORDER-001")
                .build();
        when(paymentGateway.createPaymentRequest(order)).thenReturn(expectedVO);
        when(paymentGateway.provider()).thenReturn("ECPAY");

        OrderPaymentVO vo = orderPaymentService.requestPayment(order);

        assertNotNull(vo);
        assertEquals(expectedVO, vo);
        verify(paymentGateway).createPaymentRequest(order);
        verify(orderMapper, never()).markPaymentSucceededByNumber(anyString(), any(), any(), any(), any(), any());
        PaymentEvent event = singlePaymentEvent();
        assertEquals(PaymentEvent.EVENT_REQUEST_CREATED, event.getEventType());
        assertEquals(PaymentEvent.RESULT_PENDING, event.getResult());
        assertEquals("ECPAY", event.getProvider());
        assertEquals("ecpay-form:ORDER-001", event.getProviderReference());
        assertEquals(order.getAmount(), event.getAmount());
        assertNotNull(event.getRawPayload());
        verifyNoInteractions(webSocketServer);
    }

    @Test
    void requestPaymentShouldCompleteImmediatelyWhenDemoGatewayRequiresIt() {
        Orders order = orderWithStatus(5L, Orders.PENDING_PAYMENT);
        order.setNumber("ORDER-005");
        order.setPayStatus(Orders.UN_PAID);
        OrderPaymentVO expectedVO = OrderPaymentVO.builder()
                .packageStr("demo-paid:ORDER-005")
                .build();
        when(paymentGateway.createPaymentRequest(order)).thenReturn(expectedVO);
        when(paymentGateway.completesPaymentOnRequest()).thenReturn(true);
        when(paymentGateway.provider()).thenReturn("DEMO");
        when(orderMapper.getByNumber("ORDER-005")).thenReturn(order);
        when(orderMapper.markPaymentSucceededByNumber(eq("ORDER-005"), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                eq(Orders.TO_BE_CONFIRMED), eq(Orders.PAID), any(LocalDateTime.class))).thenReturn(1);

        OrderPaymentVO vo = orderPaymentService.requestPayment(order);

        assertEquals(expectedVO, vo);
        verify(orderMapper).markPaymentSucceededByNumber(eq("ORDER-005"), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                eq(Orders.TO_BE_CONFIRMED), eq(Orders.PAID), any(LocalDateTime.class));
        List<PaymentEvent> events = capturedPaymentEvents();
        assertEquals(2, events.size());
        assertEquals(PaymentEvent.EVENT_REQUEST_CREATED, events.get(0).getEventType());
        assertEquals(PaymentEvent.EVENT_CALLBACK_SUCCEEDED, events.get(1).getEventType());
        assertEquals(PaymentEvent.RESULT_SUCCEEDED, events.get(1).getResult());
        verify(webSocketServer).sendToAllClient(anyString());
    }

    @Test
    void handlePaymentSuccessShouldUpdatePendingOrderAndNotifyAdmin() {
        Orders order = orderWithStatus(2L, Orders.PENDING_PAYMENT);
        order.setNumber("ORDER-002");
        order.setPayStatus(Orders.UN_PAID);
        when(orderMapper.getByNumber("ORDER-002")).thenReturn(order);
        when(orderMapper.markPaymentSucceededByNumber(eq("ORDER-002"), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                eq(Orders.TO_BE_CONFIRMED), eq(Orders.PAID), any(LocalDateTime.class))).thenReturn(1);

        orderPaymentService.handlePaymentSuccess("ORDER-002");

        verify(orderMapper).markPaymentSucceededByNumber(eq("ORDER-002"), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                eq(Orders.TO_BE_CONFIRMED), eq(Orders.PAID), any(LocalDateTime.class));
        PaymentEvent event = singlePaymentEvent();
        assertEquals(PaymentEvent.EVENT_CALLBACK_SUCCEEDED, event.getEventType());
        assertEquals(PaymentEvent.RESULT_SUCCEEDED, event.getResult());
        assertEquals("ORDER-002", event.getOrderNumber());
        verify(webSocketServer).sendToAllClient(anyString());
    }

    @Test
    void handlePaymentSuccessShouldIgnoreDuplicatePaidCallback() {
        Orders order = orderWithStatus(3L, Orders.CONFIRMED);
        order.setNumber("ORDER-003");
        order.setPayStatus(Orders.PAID);
        when(orderMapper.getByNumber("ORDER-003")).thenReturn(order);

        orderPaymentService.handlePaymentSuccess("ORDER-003");

        verify(orderMapper, never()).markPaymentSucceededByNumber(anyString(), any(), any(), any(), any(), any());
        PaymentEvent event = singlePaymentEvent();
        assertEquals(PaymentEvent.EVENT_CALLBACK_DUPLICATE, event.getEventType());
        assertEquals(PaymentEvent.RESULT_IGNORED, event.getResult());
        verifyNoInteractions(webSocketServer);
    }

    @Test
    void handlePaymentSuccessShouldNotNotifyWhenConcurrentCallbackAlreadyMarkedPaid() {
        Orders unpaidOrder = orderWithStatus(6L, Orders.PENDING_PAYMENT);
        unpaidOrder.setNumber("ORDER-006");
        unpaidOrder.setPayStatus(Orders.UN_PAID);
        Orders paidOrder = orderWithStatus(6L, Orders.TO_BE_CONFIRMED);
        paidOrder.setNumber("ORDER-006");
        paidOrder.setPayStatus(Orders.PAID);
        when(orderMapper.getByNumber("ORDER-006")).thenReturn(unpaidOrder, paidOrder);
        when(orderMapper.markPaymentSucceededByNumber(eq("ORDER-006"), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                eq(Orders.TO_BE_CONFIRMED), eq(Orders.PAID), any(LocalDateTime.class))).thenReturn(0);

        orderPaymentService.handlePaymentSuccess("ORDER-006");

        verify(orderMapper).markPaymentSucceededByNumber(eq("ORDER-006"), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                eq(Orders.TO_BE_CONFIRMED), eq(Orders.PAID), any(LocalDateTime.class));
        PaymentEvent event = singlePaymentEvent();
        assertEquals(PaymentEvent.EVENT_CALLBACK_DUPLICATE, event.getEventType());
        assertEquals(PaymentEvent.RESULT_IGNORED, event.getResult());
        verifyNoInteractions(webSocketServer);
    }

    @Test
    void handlePaymentSuccessShouldRejectMissingOrder() {
        when(orderMapper.getByNumber("ORDER-MISSING")).thenReturn(null);

        OrderBusinessException exception = assertThrows(OrderBusinessException.class,
                () -> orderPaymentService.handlePaymentSuccess("ORDER-MISSING"));

        assertEquals(MessageConstant.ORDER_NOT_FOUND, exception.getMessage());
        PaymentEvent event = singlePaymentEvent();
        assertEquals(PaymentEvent.EVENT_CALLBACK_REJECTED, event.getEventType());
        assertEquals(PaymentEvent.RESULT_REJECTED, event.getResult());
        assertEquals("ORDER-MISSING", event.getOrderNumber());
        verifyNoInteractions(webSocketServer);
    }

    @Test
    void handlePaymentSuccessShouldRejectUnpaidOrderThatCannotTransitionToPaid() {
        Orders order = orderWithStatus(4L, Orders.CONFIRMED);
        order.setNumber("ORDER-004");
        order.setPayStatus(Orders.UN_PAID);
        when(orderMapper.getByNumber("ORDER-004")).thenReturn(order);
        when(orderMapper.markPaymentSucceededByNumber(eq("ORDER-004"), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                eq(Orders.TO_BE_CONFIRMED), eq(Orders.PAID), any(LocalDateTime.class))).thenReturn(0);

        OrderBusinessException exception = assertThrows(OrderBusinessException.class,
                () -> orderPaymentService.handlePaymentSuccess("ORDER-004"));

        assertEquals(MessageConstant.ORDER_STATUS_ERROR, exception.getMessage());
        PaymentEvent event = singlePaymentEvent();
        assertEquals(PaymentEvent.EVENT_CALLBACK_REJECTED, event.getEventType());
        assertEquals(PaymentEvent.RESULT_REJECTED, event.getResult());
        verifyNoInteractions(webSocketServer);
    }

    private PaymentEvent singlePaymentEvent() {
        List<PaymentEvent> events = capturedPaymentEvents();
        assertEquals(1, events.size());
        return events.get(0);
    }

    private List<PaymentEvent> capturedPaymentEvents() {
        ArgumentCaptor<PaymentEvent> captor = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(paymentEventMapper, org.mockito.Mockito.atLeastOnce()).insert(captor.capture());
        return captor.getAllValues();
    }

    private static Orders orderWithStatus(Long id, Integer status) {
        Orders order = new Orders();
        order.setId(id);
        order.setStatus(status);
        return order;
    }
}
