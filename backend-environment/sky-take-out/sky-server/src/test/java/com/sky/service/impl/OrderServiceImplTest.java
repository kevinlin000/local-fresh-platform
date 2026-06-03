package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.Orders;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.OrderMapper;
import com.sky.service.OrderCancellationService;
import com.sky.service.OrderFulfillmentService;
import com.sky.service.OrderPaymentService;
import com.sky.service.OrderQueryService;
import com.sky.service.OrderSubmissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private OrderCancellationService orderCancellationService;

    @Mock
    private OrderPaymentService orderPaymentService;

    @Mock
    private OrderQueryService orderQueryService;

    @Mock
    private OrderSubmissionService orderSubmissionService;

    @Mock
    private OrderFulfillmentService orderFulfillmentService;

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
        verifyNoInteractions(orderPaymentService);
    }

    @Test
    void paymentShouldMovePendingUnpaidOrderToWaitingForAcceptance() throws Exception {
        BaseContext.setCurrentId(100L);
        Orders order = orderWithStatus(1L, Orders.PENDING_PAYMENT);
        order.setUserId(100L);
        order.setNumber("ORDER-002");
        order.setPayStatus(Orders.UN_PAID);
        when(orderMapper.getByNumber("ORDER-002")).thenReturn(order);
        orderService.payment(paymentDTO("ORDER-002"));

        verify(orderPaymentService).requestPayment(order);
    }

    @Test
    void paySuccessShouldDelegatePaymentCallback() {
        orderService.paySuccess("ORDER-003");

        verify(orderPaymentService).handlePaymentSuccess("ORDER-003");
    }

    @Test
    void confirmShouldDelegateToFulfillmentService() {
        OrdersConfirmDTO dto = new OrdersConfirmDTO();
        dto.setId(10L);

        orderService.confirm(dto);

        verify(orderFulfillmentService).confirm(dto);
    }

    @Test
    void deliveryShouldDelegateToFulfillmentService() {
        orderService.delivery(41L);

        verify(orderFulfillmentService).delivery(41L);
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
        verifyNoInteractions(orderCancellationService);
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
        verifyNoInteractions(orderCancellationService);
    }

    @Test
    void adminCancelShouldSetCancelMetadata() throws Exception {
        Orders order = orderWithStatus(31L, Orders.CONFIRMED);
        order.setPayStatus(Orders.UN_PAID);
        when(orderMapper.getById(31L)).thenReturn(order);

        orderService.cancel(cancelDTO(31L, "stock unavailable"));

        verify(orderCancellationService).cancelOrder(eq(order), eq("stock unavailable"), eq(null), eq("ADMIN"), eq(null));
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
    void completeShouldDelegateToFulfillmentService() {
        orderService.complete(51L);

        verify(orderFulfillmentService).complete(51L);
    }

    @Test
    void detailsShouldDelegateToQueryService() {
        orderService.details(99L);

        verify(orderQueryService).details(99L);
    }

    @Test
    void submitShouldDelegateToSubmissionService() {
        OrdersSubmitDTO dto = new OrdersSubmitDTO();

        orderService.submitOrder(dto);

        verify(orderSubmissionService).submitOrder(dto);
    }

    @Test
    void reminderShouldDelegateToFulfillmentService() {
        orderService.reminder(61L);

        verify(orderFulfillmentService).reminder(61L);
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
