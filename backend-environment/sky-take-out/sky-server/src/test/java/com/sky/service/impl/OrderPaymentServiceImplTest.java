package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.entity.Orders;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.OrderMapper;
import com.sky.vo.OrderPaymentVO;
import com.sky.websocket.WebSocketServer;
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

    @InjectMocks
    private OrderPaymentServiceImpl orderPaymentService;

    @Test
    void requestPaymentShouldMarkOrderPaidAndReturnPaymentVO() {
        Orders order = orderWithStatus(1L, Orders.PENDING_PAYMENT);
        order.setNumber("ORDER-001");
        when(orderMapper.markPaymentSucceededByNumber(eq("ORDER-001"), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                eq(Orders.TO_BE_CONFIRMED), eq(Orders.PAID), any(LocalDateTime.class))).thenReturn(1);

        OrderPaymentVO vo = orderPaymentService.requestPayment(order);

        assertNotNull(vo);
        verify(orderMapper).markPaymentSucceededByNumber(eq("ORDER-001"), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                eq(Orders.TO_BE_CONFIRMED), eq(Orders.PAID), any(LocalDateTime.class));
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
