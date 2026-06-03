package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.entity.Orders;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.OrderMapper;
import com.sky.websocket.WebSocketServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderFulfillmentServiceImplTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private WebSocketServer webSocketServer;

    @InjectMocks
    private OrderFulfillmentServiceImpl orderFulfillmentService;

    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
    }

    @Test
    void confirmShouldRejectOrderThatIsNotWaitingForAcceptance() {
        Orders completedOrder = orderWithStatus(10L, Orders.COMPLETED);
        when(orderMapper.getById(10L)).thenReturn(completedOrder);

        OrdersConfirmDTO dto = new OrdersConfirmDTO();
        dto.setId(10L);

        OrderBusinessException exception = assertThrows(OrderBusinessException.class,
                () -> orderFulfillmentService.confirm(dto));

        assertEquals(MessageConstant.ORDER_STATUS_ERROR, exception.getMessage());
        verify(orderMapper, never()).update(any(Orders.class));
    }

    @Test
    void confirmShouldUpdateOnlyWhenOrderIsWaitingForAcceptance() {
        Orders order = orderWithStatus(11L, Orders.TO_BE_CONFIRMED);
        when(orderMapper.getById(11L)).thenReturn(order);

        OrdersConfirmDTO dto = new OrdersConfirmDTO();
        dto.setId(11L);

        orderFulfillmentService.confirm(dto);

        Orders updated = captureUpdatedOrder();
        assertEquals(11L, updated.getId());
        assertEquals(Orders.CONFIRMED, updated.getStatus());
        verifyNoInteractions(webSocketServer);
    }

    @Test
    void deliveryShouldRejectOrderThatIsNotConfirmed() {
        Orders order = orderWithStatus(40L, Orders.TO_BE_CONFIRMED);
        when(orderMapper.getById(40L)).thenReturn(order);

        OrderBusinessException exception = assertThrows(OrderBusinessException.class,
                () -> orderFulfillmentService.delivery(40L));

        assertEquals(MessageConstant.ORDER_STATUS_ERROR, exception.getMessage());
        verify(orderMapper, never()).update(any(Orders.class));
    }

    @Test
    void deliveryShouldMoveConfirmedOrderToDeliveryInProgress() {
        Orders order = orderWithStatus(41L, Orders.CONFIRMED);
        when(orderMapper.getById(41L)).thenReturn(order);

        orderFulfillmentService.delivery(41L);

        Orders updated = captureUpdatedOrder();
        assertEquals(41L, updated.getId());
        assertEquals(Orders.DELIVERY_IN_PROGRESS, updated.getStatus());
    }

    @Test
    void completeShouldRejectOrderThatIsNotInDelivery() {
        Orders order = orderWithStatus(50L, Orders.CONFIRMED);
        when(orderMapper.getById(50L)).thenReturn(order);

        OrderBusinessException exception = assertThrows(OrderBusinessException.class,
                () -> orderFulfillmentService.complete(50L));

        assertEquals(MessageConstant.ORDER_STATUS_ERROR, exception.getMessage());
        verify(orderMapper, never()).update(any(Orders.class));
    }

    @Test
    void completeShouldSetCompletedStatusAndDeliveryTime() {
        Orders order = orderWithStatus(51L, Orders.DELIVERY_IN_PROGRESS);
        when(orderMapper.getById(51L)).thenReturn(order);

        orderFulfillmentService.complete(51L);

        Orders updated = captureUpdatedOrder();
        assertEquals(51L, updated.getId());
        assertEquals(Orders.COMPLETED, updated.getStatus());
        assertNotNull(updated.getDeliveryTime());
    }

    @Test
    void reminderShouldRejectOrderOwnedByAnotherUser() {
        BaseContext.setCurrentId(100L);
        Orders order = orderWithStatus(61L, Orders.CONFIRMED);
        order.setUserId(200L);
        when(orderMapper.getById(61L)).thenReturn(order);

        OrderBusinessException exception = assertThrows(OrderBusinessException.class,
                () -> orderFulfillmentService.reminder(61L));

        assertEquals(MessageConstant.ORDER_NOT_FOUND, exception.getMessage());
        verifyNoInteractions(webSocketServer);
    }

    @Test
    void reminderShouldPushOrderNumberToAdminClients() {
        BaseContext.setCurrentId(100L);
        Orders order = orderWithStatus(62L, Orders.CONFIRMED);
        order.setUserId(100L);
        order.setNumber("ORDER-062");
        when(orderMapper.getById(62L)).thenReturn(order);

        orderFulfillmentService.reminder(62L);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(webSocketServer).sendToAllClient(captor.capture());
        JSONObject payload = JSON.parseObject(captor.getValue());
        assertEquals(2, payload.getInteger("type"));
        assertEquals(62L, payload.getLong("orderId"));
        assertEquals("訂單號：ORDER-062", payload.getString("content"));
    }

    private Orders captureUpdatedOrder() {
        ArgumentCaptor<Orders> captor = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).update(captor.capture());
        return captor.getValue();
    }

    private static Orders orderWithStatus(Long id, Integer status) {
        Orders order = new Orders();
        order.setId(id);
        order.setStatus(status);
        return order;
    }
}
