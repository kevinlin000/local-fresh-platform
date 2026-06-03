package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.entity.Orders;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.service.InventoryService;
import com.sky.service.OrderCancellationService;
import com.sky.utils.WeChatPayUtil;
import com.sky.websocket.WebSocketServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderDetailMapper orderDetailMapper;

    @Mock
    private WeChatPayUtil weChatPayUtil;

    @Mock
    private WebSocketServer webSocketServer;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private OrderCancellationService orderCancellationService;

    @InjectMocks
    private OrderServiceImpl orderService;

    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
    }

    @Test
    void paymentShouldRejectOrderOwnedByAnotherUser() {
        BaseContext.setCurrentId(100L);
        Orders order = orderWithStatus(1L, Orders.PENDING_PAYMENT);
        order.setUserId(200L);
        order.setNumber("ORDER-001");
        order.setPayStatus(Orders.UN_PAID);
        when(orderMapper.getByNumber("ORDER-001")).thenReturn(order);

        OrdersPaymentDTO dto = paymentDTO("ORDER-001");

        OrderBusinessException exception = assertThrows(OrderBusinessException.class,
                () -> orderService.payment(dto));

        assertEquals(MessageConstant.ORDER_NOT_FOUND, exception.getMessage());
        verify(orderMapper, never()).markPaymentSucceededByNumber(anyString(), any(), any(), any(), any(), any());
    }

    @Test
    void paymentShouldMovePendingUnpaidOrderToWaitingForAcceptance() throws Exception {
        BaseContext.setCurrentId(100L);
        Orders order = orderWithStatus(1L, Orders.PENDING_PAYMENT);
        order.setUserId(100L);
        order.setNumber("ORDER-002");
        order.setPayStatus(Orders.UN_PAID);
        when(orderMapper.getByNumber("ORDER-002")).thenReturn(order);
        when(orderMapper.markPaymentSucceededByNumber(eq("ORDER-002"), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                eq(Orders.TO_BE_CONFIRMED), eq(Orders.PAID), any(LocalDateTime.class))).thenReturn(1);

        orderService.payment(paymentDTO("ORDER-002"));

        verify(orderMapper).markPaymentSucceededByNumber(eq("ORDER-002"), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                eq(Orders.TO_BE_CONFIRMED), eq(Orders.PAID), any(LocalDateTime.class));
    }

    @Test
    void paySuccessShouldUpdatePendingOrderAndNotifyAdmin() {
        Orders order = orderWithStatus(2L, Orders.PENDING_PAYMENT);
        order.setNumber("ORDER-003");
        order.setPayStatus(Orders.UN_PAID);
        when(orderMapper.getByNumber("ORDER-003")).thenReturn(order);
        when(orderMapper.markPaymentSucceededByNumber(eq("ORDER-003"), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                eq(Orders.TO_BE_CONFIRMED), eq(Orders.PAID), any(LocalDateTime.class))).thenReturn(1);

        orderService.paySuccess("ORDER-003");

        verify(orderMapper).markPaymentSucceededByNumber(eq("ORDER-003"), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                eq(Orders.TO_BE_CONFIRMED), eq(Orders.PAID), any(LocalDateTime.class));
        verify(webSocketServer).sendToAllClient(anyString());
    }

    @Test
    void paySuccessShouldIgnoreDuplicatePaidCallback() {
        Orders order = orderWithStatus(2L, Orders.CONFIRMED);
        order.setNumber("ORDER-003");
        order.setPayStatus(Orders.PAID);
        when(orderMapper.getByNumber("ORDER-003")).thenReturn(order);

        orderService.paySuccess("ORDER-003");

        verify(orderMapper, never()).markPaymentSucceededByNumber(anyString(), any(), any(), any(), any(), any());
        verify(orderMapper, never()).update(any(Orders.class));
        verifyNoInteractions(webSocketServer);
    }

    @Test
    void paySuccessShouldRejectUnpaidOrderThatCannotTransitionToPaid() {
        Orders order = orderWithStatus(2L, Orders.CONFIRMED);
        order.setNumber("ORDER-003");
        order.setPayStatus(Orders.UN_PAID);
        when(orderMapper.getByNumber("ORDER-003")).thenReturn(order);
        when(orderMapper.markPaymentSucceededByNumber(eq("ORDER-003"), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                eq(Orders.TO_BE_CONFIRMED), eq(Orders.PAID), any(LocalDateTime.class))).thenReturn(0);

        OrderBusinessException exception = assertThrows(OrderBusinessException.class,
                () -> orderService.paySuccess("ORDER-003"));

        assertEquals(MessageConstant.ORDER_STATUS_ERROR, exception.getMessage());
        verifyNoInteractions(webSocketServer);
    }

    @Test
    void confirmShouldRejectOrderThatIsNotWaitingForAcceptance() {
        Orders completedOrder = orderWithStatus(10L, Orders.COMPLETED);
        when(orderMapper.getById(10L)).thenReturn(completedOrder);

        OrdersConfirmDTO dto = new OrdersConfirmDTO();
        dto.setId(10L);

        OrderBusinessException exception = assertThrows(OrderBusinessException.class,
                () -> orderService.confirm(dto));

        assertEquals(MessageConstant.ORDER_STATUS_ERROR, exception.getMessage());
        verify(orderMapper, never()).update(any(Orders.class));
    }

    @Test
    void confirmShouldUpdateOnlyWhenOrderIsWaitingForAcceptance() {
        Orders order = orderWithStatus(11L, Orders.TO_BE_CONFIRMED);
        when(orderMapper.getById(11L)).thenReturn(order);

        OrdersConfirmDTO dto = new OrdersConfirmDTO();
        dto.setId(11L);

        orderService.confirm(dto);

        verify(orderMapper).update(any(Orders.class));
        verifyNoInteractions(orderDetailMapper);
        verifyNoInteractions(inventoryService);
    }

    @Test
    void userCancelShouldRejectOrderOwnedByAnotherUser() throws Exception {
        BaseContext.setCurrentId(100L);
        Orders order = orderWithStatus(20L, Orders.PENDING_PAYMENT);
        order.setUserId(200L);
        when(orderMapper.getById(20L)).thenReturn(order);

        OrderBusinessException exception = assertThrows(OrderBusinessException.class,
                () -> orderService.userCancelById(20L));

        assertEquals(MessageConstant.ORDER_NOT_FOUND, exception.getMessage());
        verify(orderMapper, never()).update(any(Orders.class));
        verifyNoInteractions(weChatPayUtil);
    }

    @Test
    void userCancelShouldRefundPaidWaitingOrder() throws Exception {
        BaseContext.setCurrentId(100L);
        Orders order = orderWithStatus(21L, Orders.TO_BE_CONFIRMED);
        order.setUserId(100L);
        order.setNumber("ORDER-004");
        order.setPayStatus(Orders.PAID);
        order.setAmount(new BigDecimal("128.00"));
        when(orderMapper.getById(21L)).thenReturn(order);
        orderService.userCancelById(21L);

        verify(orderCancellationService).cancelOrder(eq(order), eq("用户取消"), eq(null), eq("MEMBER"), eq(100L));
    }

    @Test
    void adminRejectShouldDelegateCancellation() throws Exception {
        Orders order = orderWithStatus(22L, Orders.TO_BE_CONFIRMED);
        order.setNumber("ORDER-005");
        order.setPayStatus(Orders.PAID);
        order.setAmount(new BigDecimal("256.00"));
        when(orderMapper.getById(22L)).thenReturn(order);

        OrdersRejectionDTO dto = new OrdersRejectionDTO();
        dto.setId(22L);
        dto.setRejectionReason("商品售完");
        orderService.rejection(dto);

        verify(orderCancellationService).cancelOrder(eq(order), eq(null), eq("商品售完"), eq("ADMIN"), eq(null));
    }

    @Test
    void adminCancelShouldRejectCompletedOrder() throws Exception {
        Orders order = orderWithStatus(30L, Orders.COMPLETED);
        when(orderMapper.getById(30L)).thenReturn(order);

        OrderBusinessException exception = assertThrows(OrderBusinessException.class,
                () -> orderService.cancel(cancelDTO(30L, "test")));

        assertEquals(MessageConstant.ORDER_STATUS_ERROR, exception.getMessage());
        verify(orderMapper, never()).update(any(Orders.class));
        verifyNoInteractions(weChatPayUtil);
    }

    @Test
    void adminCancelShouldSetCancelMetadata() throws Exception {
        Orders order = orderWithStatus(31L, Orders.CONFIRMED);
        order.setPayStatus(Orders.UN_PAID);
        when(orderMapper.getById(31L)).thenReturn(order);

        orderService.cancel(cancelDTO(31L, "stock unavailable"));

        verify(orderCancellationService).cancelOrder(eq(order), eq("stock unavailable"), eq(null), eq("ADMIN"), eq(null));
        verifyNoInteractions(weChatPayUtil);
    }

    @Test
    void adminCancelShouldRefundActualAmountWhenPaid() throws Exception {
        Orders order = orderWithStatus(32L, Orders.CONFIRMED);
        order.setNumber("ORDER-006");
        order.setPayStatus(Orders.PAID);
        order.setAmount(new BigDecimal("99.00"));
        when(orderMapper.getById(32L)).thenReturn(order);
        orderService.cancel(cancelDTO(32L, "merchant closed"));

        verify(orderCancellationService).cancelOrder(eq(order), eq("merchant closed"), eq(null), eq("ADMIN"), eq(null));
    }

    @Test
    void deliveryShouldRejectOrderThatIsNotConfirmed() {
        Orders order = orderWithStatus(40L, Orders.TO_BE_CONFIRMED);
        when(orderMapper.getById(40L)).thenReturn(order);

        OrderBusinessException exception = assertThrows(OrderBusinessException.class,
                () -> orderService.delivery(40L));

        assertEquals(MessageConstant.ORDER_STATUS_ERROR, exception.getMessage());
        verify(orderMapper, never()).update(any(Orders.class));
    }

    @Test
    void deliveryShouldMoveConfirmedOrderToDeliveryInProgress() {
        Orders order = orderWithStatus(41L, Orders.CONFIRMED);
        when(orderMapper.getById(41L)).thenReturn(order);

        orderService.delivery(41L);

        Orders updated = captureUpdatedOrder();
        assertEquals(Orders.DELIVERY_IN_PROGRESS, updated.getStatus());
    }

    @Test
    void completeShouldRejectOrderThatIsNotInDelivery() {
        Orders order = orderWithStatus(50L, Orders.CONFIRMED);
        when(orderMapper.getById(50L)).thenReturn(order);

        OrderBusinessException exception = assertThrows(OrderBusinessException.class,
                () -> orderService.complete(50L));

        assertEquals(MessageConstant.ORDER_STATUS_ERROR, exception.getMessage());
        verify(orderMapper, never()).update(any(Orders.class));
    }

    @Test
    void completeShouldSetCompletedStatusAndDeliveryTime() {
        Orders order = orderWithStatus(51L, Orders.DELIVERY_IN_PROGRESS);
        when(orderMapper.getById(51L)).thenReturn(order);

        orderService.complete(51L);

        Orders updated = captureUpdatedOrder();
        assertEquals(Orders.COMPLETED, updated.getStatus());
        assertNotNull(updated.getDeliveryTime());
    }

    @Test
    void detailsShouldRejectMissingOrderWithoutQueryingDetails() {
        when(orderMapper.getById(99L)).thenReturn(null);

        OrderBusinessException exception = assertThrows(OrderBusinessException.class,
                () -> orderService.details(99L));

        assertEquals(MessageConstant.ORDER_NOT_FOUND, exception.getMessage());
        verifyNoInteractions(orderDetailMapper);
    }

    private Orders captureUpdatedOrder() {
        ArgumentCaptor<Orders> captor = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).update(captor.capture());
        return captor.getValue();
    }

    private static OrdersPaymentDTO paymentDTO(String orderNumber) {
        OrdersPaymentDTO dto = new OrdersPaymentDTO();
        dto.setOrderNumber(orderNumber);
        dto.setPayMethod(1);
        return dto;
    }

    private static OrdersCancelDTO cancelDTO(Long id, String reason) {
        OrdersCancelDTO dto = new OrdersCancelDTO();
        dto.setId(id);
        dto.setCancelReason(reason);
        return dto;
    }

    private static Orders orderWithStatus(Long id, Integer status) {
        Orders order = new Orders();
        order.setId(id);
        order.setStatus(status);
        return order;
    }
}
