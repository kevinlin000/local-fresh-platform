package com.localfresh.service.impl;

import com.localfresh.constant.MessageConstant;
import com.localfresh.entity.Orders;
import com.localfresh.exception.OrderBusinessException;
import com.localfresh.mapper.OrderMapper;
import com.localfresh.service.payment.PaymentGateway;
import com.localfresh.vo.OrderPaymentVO;
import com.localfresh.websocket.WebSocketServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

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

    @InjectMocks
    private OrderPaymentServiceImpl orderPaymentService;

    @Test
    void requestPaymentShouldMarkOrderPaidAndReturnPaymentVO() {
        Orders order = orderWithStatus(1L, Orders.PENDING_PAYMENT);
        order.setNumber("ORDER-001");
        OrderPaymentVO expectedVO = OrderPaymentVO.builder()
                .packageStr("demo-paid:ORDER-001")
                .build();
        when(orderMapper.markPaymentSucceededByNumber(eq("ORDER-001"), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                eq(Orders.TO_BE_CONFIRMED), eq(Orders.PAID), any(LocalDateTime.class))).thenReturn(1);
        when(paymentGateway.createPaymentRequest(order)).thenReturn(expectedVO);

        OrderPaymentVO vo = orderPaymentService.requestPayment(order);

        assertNotNull(vo);
        assertEquals(expectedVO, vo);
        verify(orderMapper).markPaymentSucceededByNumber(eq("ORDER-001"), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                eq(Orders.TO_BE_CONFIRMED), eq(Orders.PAID), any(LocalDateTime.class));
        verify(paymentGateway).createPaymentRequest(order);
    }

    @Test
    void requestPaymentShouldRejectOrderThatCannotBeMarkedPaid() {
        Orders order = orderWithStatus(5L, Orders.CONFIRMED);
        order.setNumber("ORDER-005");
        order.setPayStatus(Orders.UN_PAID);
        when(orderMapper.markPaymentSucceededByNumber(eq("ORDER-005"), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                eq(Orders.TO_BE_CONFIRMED), eq(Orders.PAID), any(LocalDateTime.class))).thenReturn(0);
        when(orderMapper.getByNumber("ORDER-005")).thenReturn(order);

        OrderBusinessException exception = assertThrows(OrderBusinessException.class,
                () -> orderPaymentService.requestPayment(order));

        assertEquals(MessageConstant.ORDER_STATUS_ERROR, exception.getMessage());
        verifyNoInteractions(paymentGateway);
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
        verifyNoInteractions(webSocketServer);
    }

    @Test
    void handlePaymentSuccessShouldRejectMissingOrder() {
        when(orderMapper.getByNumber("ORDER-MISSING")).thenReturn(null);

        OrderBusinessException exception = assertThrows(OrderBusinessException.class,
                () -> orderPaymentService.handlePaymentSuccess("ORDER-MISSING"));

        assertEquals(MessageConstant.ORDER_NOT_FOUND, exception.getMessage());
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
        verifyNoInteractions(webSocketServer);
    }

    private static Orders orderWithStatus(Long id, Integer status) {
        Orders order = new Orders();
        order.setId(id);
        order.setStatus(status);
        return order;
    }
}
