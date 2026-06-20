package com.localfresh.service.impl;

import com.localfresh.entity.GiftBoxProduct;
import com.localfresh.entity.OrderDetail;
import com.localfresh.entity.Orders;
import com.localfresh.mapper.GiftBoxProductMapper;
import com.localfresh.mapper.OrderDetailMapper;
import com.localfresh.mapper.OrderMapper;
import com.localfresh.mapper.ProductInventoryLogMapper;
import com.localfresh.service.InventoryService;
import com.localfresh.service.payment.PaymentGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCancellationServiceImplTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderDetailMapper orderDetailMapper;

    @Mock
    private GiftBoxProductMapper giftBoxProductMapper;

    @Mock
    private ProductInventoryLogMapper productInventoryLogMapper;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private OrderCancellationServiceImpl orderCancellationService;

    @Test
    void cancelOrder_refundsPaidOrderUpdatesMetadataAndRestoresStock() throws Exception {
        Orders order = new Orders();
        order.setId(10L);
        order.setNumber("ORDER-010");
        order.setPayStatus(Orders.PAID);
        order.setAmount(new BigDecimal("188.00"));
        when(orderDetailMapper.getByOrderId(10L)).thenReturn(List.of(OrderDetail.builder()
                .productId(20L)
                .number(2)
                .build()));

        orderCancellationService.cancelOrder(order, "店家取消", null, "ADMIN", 1L);

        verify(paymentGateway).refund(order, "店家取消");
        ArgumentCaptor<Orders> captor = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).update(captor.capture());
        Orders updated = captor.getValue();
        assertEquals(10L, updated.getId());
        assertEquals(Orders.CANCELLED, updated.getStatus());
        assertEquals(Orders.REFUND, updated.getPayStatus());
        assertEquals("店家取消", updated.getCancelReason());
        assertNotNull(updated.getCancelTime());
        verify(inventoryService).restoreProduct(20L, 2, "ORDER_CANCEL_RESTORE", 10L, "ADMIN", 1L);
    }

    @Test
    void cancelOrder_recordsRejectionWithoutRefundingUnpaidOrder() throws Exception {
        Orders order = new Orders();
        order.setId(11L);
        order.setNumber("ORDER-011");
        order.setPayStatus(Orders.UN_PAID);
        when(orderDetailMapper.getByOrderId(11L)).thenReturn(List.of());

        orderCancellationService.cancelOrder(order, null, "商品售完", "ADMIN", 2L);

        verify(paymentGateway, never()).refund(order, "商品售完");
        ArgumentCaptor<Orders> captor = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).update(captor.capture());
        Orders updated = captor.getValue();
        assertEquals(11L, updated.getId());
        assertEquals(Orders.CANCELLED, updated.getStatus());
        assertEquals("商品售完", updated.getRejectionReason());
        assertNotNull(updated.getCancelTime());
    }

    @Test
    void cancelOrder_restoresGiftBoxComponentStock() throws Exception {
        Orders order = new Orders();
        order.setId(12L);
        order.setNumber("ORDER-012");
        order.setPayStatus(Orders.UN_PAID);
        when(orderDetailMapper.getByOrderId(12L)).thenReturn(List.of(OrderDetail.builder()
                .giftBoxId(30L)
                .number(2)
                .build()));
        when(giftBoxProductMapper.getBySetmealId(30L)).thenReturn(List.of(
                GiftBoxProduct.builder().productId(40L).copies(3).build(),
                GiftBoxProduct.builder().productId(41L).copies(1).build()
        ));

        orderCancellationService.cancelOrder(order, "會員取消", null, "MEMBER", 3L);

        verify(inventoryService).restoreProduct(40L, 6, "ORDER_CANCEL_RESTORE", 12L, "MEMBER", 3L);
        verify(inventoryService).restoreProduct(41L, 2, "ORDER_CANCEL_RESTORE", 12L, "MEMBER", 3L);
    }

    @Test
    void cancelOrder_skipsDuplicateWhenOrderIsAlreadyCancelled() throws Exception {
        Orders staleOrder = new Orders();
        staleOrder.setId(13L);
        staleOrder.setNumber("ORDER-013");
        staleOrder.setPayStatus(Orders.PAID);
        staleOrder.setAmount(new BigDecimal("199.00"));
        Orders cancelledOrder = new Orders();
        cancelledOrder.setId(13L);
        cancelledOrder.setStatus(Orders.CANCELLED);
        when(orderMapper.getById(13L)).thenReturn(cancelledOrder);

        orderCancellationService.cancelOrder(staleOrder, "重複取消", null, "ADMIN", 4L);

        verify(paymentGateway, never()).refund(staleOrder, "重複取消");
        verify(orderMapper, never()).update(org.mockito.ArgumentMatchers.any(Orders.class));
        verify(orderDetailMapper, never()).getByOrderId(13L);
    }

    @Test
    void cancelOrder_skipsDuplicateWhenRestoreLogAlreadyExists() throws Exception {
        Orders order = new Orders();
        order.setId(14L);
        order.setNumber("ORDER-014");
        order.setPayStatus(Orders.PAID);
        order.setAmount(new BigDecimal("288.00"));
        when(orderMapper.getById(14L)).thenReturn(order);
        when(productInventoryLogMapper.countByReferenceAndReason("ORDER", 14L, "ORDER_CANCEL_RESTORE"))
                .thenReturn(1);

        orderCancellationService.cancelOrder(order, "重複取消", null, "ADMIN", 5L);

        verify(paymentGateway, never()).refund(order, "重複取消");
        verify(orderMapper, never()).update(org.mockito.ArgumentMatchers.any(Orders.class));
        verify(orderDetailMapper, never()).getByOrderId(14L);
    }
}
