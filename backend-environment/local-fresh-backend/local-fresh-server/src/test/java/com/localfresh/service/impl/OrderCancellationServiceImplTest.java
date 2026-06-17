package com.localfresh.service.impl;

import com.localfresh.entity.OrderDetail;
import com.localfresh.entity.Orders;
import com.localfresh.mapper.GiftBoxProductMapper;
import com.localfresh.mapper.OrderDetailMapper;
import com.localfresh.mapper.OrderMapper;
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

        orderCancellationService.cancelOrder(order, "商家取消", null, "ADMIN", 1L);

        verify(paymentGateway).refund(order, "商家取消");
        ArgumentCaptor<Orders> captor = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).update(captor.capture());
        Orders updated = captor.getValue();
        assertEquals(10L, updated.getId());
        assertEquals(Orders.CANCELLED, updated.getStatus());
        assertEquals(Orders.REFUND, updated.getPayStatus());
        assertEquals("商家取消", updated.getCancelReason());
        assertNotNull(updated.getCancelTime());
        verify(inventoryService).restoreProduct(20L, 2, "ORDER_CANCEL_RESTORE", 10L, "ADMIN", 1L);
    }
}
