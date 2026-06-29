package com.localfresh.service.impl;

import com.localfresh.constant.MessageConstant;
import com.localfresh.context.BaseContext;
import com.localfresh.dto.OrdersSubmitDTO;
import com.localfresh.entity.Cart;
import com.localfresh.entity.GiftBoxProduct;
import com.localfresh.entity.OrderDetail;
import com.localfresh.entity.Orders;
import com.localfresh.entity.ShippingAddress;
import com.localfresh.exception.AddressBookBusinessException;
import com.localfresh.exception.OrderBusinessException;
import com.localfresh.mapper.CartMapper;
import com.localfresh.mapper.GiftBoxProductMapper;
import com.localfresh.mapper.OrderDetailMapper;
import com.localfresh.mapper.OrderMapper;
import com.localfresh.mapper.ShippingAddressMapper;
import com.localfresh.service.InventoryService;
import com.localfresh.vo.OrderSubmitVO;
import org.junit.jupiter.api.AfterEach;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderSubmissionServiceImplTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderDetailMapper orderDetailMapper;

    @Mock
    private ShippingAddressMapper shippingAddressMapper;

    @Mock
    private CartMapper cartMapper;

    @Mock
    private GiftBoxProductMapper giftBoxProductMapper;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private OrderSubmissionServiceImpl orderSubmissionService;

    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
    }

    @Test
    void submitShouldRejectMissingAddressBeforeReadingCart() {
        BaseContext.setCurrentId(100L);
        OrdersSubmitDTO dto = submitDTO();
        when(shippingAddressMapper.getById(10L)).thenReturn(null);

        AddressBookBusinessException exception = assertThrows(AddressBookBusinessException.class,
                () -> orderSubmissionService.submitOrder(dto));

        assertEquals(MessageConstant.ADDRESS_BOOK_IS_NULL, exception.getMessage());
        verifyNoInteractions(cartMapper, orderMapper, orderDetailMapper, inventoryService);
    }

    @Test
    void submitShouldRejectAddressOwnedByAnotherUser() {
        BaseContext.setCurrentId(100L);
        OrdersSubmitDTO dto = submitDTO();
        ShippingAddress address = shippingAddress(200L);
        when(shippingAddressMapper.getById(10L)).thenReturn(address);

        AddressBookBusinessException exception = assertThrows(AddressBookBusinessException.class,
                () -> orderSubmissionService.submitOrder(dto));

        assertEquals(MessageConstant.ADDRESS_BOOK_IS_NULL, exception.getMessage());
        verifyNoInteractions(cartMapper, orderMapper, orderDetailMapper, inventoryService);
    }

    @Test
    void submitShouldRejectEmptyCartBeforeWritingOrder() {
        BaseContext.setCurrentId(100L);
        OrdersSubmitDTO dto = submitDTO();
        when(shippingAddressMapper.getById(10L)).thenReturn(shippingAddress(100L));
        when(cartMapper.list(any(Cart.class))).thenReturn(List.of());

        AddressBookBusinessException exception = assertThrows(AddressBookBusinessException.class,
                () -> orderSubmissionService.submitOrder(dto));

        assertEquals(MessageConstant.SHOPPING_CART_IS_NULL, exception.getMessage());
        verify(orderMapper, never()).insert(any(Orders.class));
        verifyNoInteractions(orderDetailMapper, inventoryService);
    }

    @Test
    void submitProductOrderShouldReserveStockWriteDetailsAndClearCart() {
        BaseContext.setCurrentId(100L);
        OrdersSubmitDTO dto = submitDTO();
        Cart cart = Cart.builder()
                .productId(5L)
                .name("高麗菜")
                .number(2)
                .amount(new BigDecimal("120.00"))
                .build();
        when(shippingAddressMapper.getById(10L)).thenReturn(shippingAddress(100L));
        when(cartMapper.list(any(Cart.class))).thenReturn(List.of(cart));
        assignOrderId(900L);

        OrderSubmitVO orderSubmitVO = orderSubmissionService.submitOrder(dto);

        assertEquals(900L, orderSubmitVO.getId());
        assertEquals(new BigDecimal("250.00"), orderSubmitVO.getOrderAmount());
        assertNotNull(orderSubmitVO.getOrderNumber());
        verify(inventoryService).reserveProduct(5L, 2, "ORDER_RESERVE", 900L, "MEMBER", 100L);
        verify(cartMapper).deleteByUserId(100L);

        Orders insertedOrder = captureInsertedOrder();
        assertEquals(Orders.PENDING_PAYMENT, insertedOrder.getStatus());
        assertEquals(Orders.UN_PAID, insertedOrder.getPayStatus());
        assertEquals("台北市大安區信義路一段1號", insertedOrder.getAddress());
        assertEquals(100L, insertedOrder.getUserId());
        assertEquals(10, insertedOrder.getPackAmount());
        assertEquals(new BigDecimal("250.00"), insertedOrder.getAmount());

        List<OrderDetail> details = captureOrderDetails();
        assertEquals(1, details.size());
        assertEquals(900L, details.get(0).getOrderId());
        assertEquals("高麗菜", details.get(0).getName());
    }

    @Test
    void submitGiftBoxOrderShouldReserveComponentStock() {
        BaseContext.setCurrentId(100L);
        OrdersSubmitDTO dto = submitDTO();
        Cart cart = Cart.builder()
                .giftBoxId(88L)
                .name("當季蔬菜箱")
                .number(3)
                .amount(new BigDecimal("600.00"))
                .build();
        when(shippingAddressMapper.getById(10L)).thenReturn(shippingAddress(100L));
        when(cartMapper.list(any(Cart.class))).thenReturn(List.of(cart));
        when(giftBoxProductMapper.getBySetmealId(88L)).thenReturn(List.of(
                GiftBoxProduct.builder().productId(11L).copies(2).build(),
                GiftBoxProduct.builder().productId(12L).copies(1).build()));
        assignOrderId(901L);

        OrderSubmitVO orderSubmitVO = orderSubmissionService.submitOrder(dto);

        assertEquals(new BigDecimal("1800.00"), orderSubmitVO.getOrderAmount());
        verify(inventoryService).reserveProduct(11L, 6, "ORDER_RESERVE", 901L, "MEMBER", 100L);
        verify(inventoryService).reserveProduct(12L, 3, "ORDER_RESERVE", 901L, "MEMBER", 100L);
        verify(cartMapper).deleteByUserId(100L);

        Orders insertedOrder = captureInsertedOrder();
        assertEquals(0, insertedOrder.getPackAmount());
        assertEquals(new BigDecimal("1800.00"), insertedOrder.getAmount());
    }

    @Test
    void submitProductOrderShouldWaivePackagingFeeWhenSubtotalReachesThreshold() {
        BaseContext.setCurrentId(100L);
        OrdersSubmitDTO dto = submitDTO();
        Cart cart = Cart.builder()
                .productId(5L)
                .name("家庭備菜組")
                .number(1)
                .amount(new BigDecimal("699.00"))
                .build();
        when(shippingAddressMapper.getById(10L)).thenReturn(shippingAddress(100L));
        when(cartMapper.list(any(Cart.class))).thenReturn(List.of(cart));
        assignOrderId(903L);

        OrderSubmitVO orderSubmitVO = orderSubmissionService.submitOrder(dto);

        assertEquals(new BigDecimal("699.00"), orderSubmitVO.getOrderAmount());
        Orders insertedOrder = captureInsertedOrder();
        assertEquals(0, insertedOrder.getPackAmount());
        assertEquals(new BigDecimal("699.00"), insertedOrder.getAmount());
    }

    @Test
    void submitGiftBoxOrderShouldRejectMissingComponents() {
        BaseContext.setCurrentId(100L);
        OrdersSubmitDTO dto = submitDTO();
        Cart cart = Cart.builder().giftBoxId(88L).number(1).build();
        when(shippingAddressMapper.getById(10L)).thenReturn(shippingAddress(100L));
        when(cartMapper.list(any(Cart.class))).thenReturn(List.of(cart));
        when(giftBoxProductMapper.getBySetmealId(88L)).thenReturn(List.of());
        assignOrderId(902L);

        OrderBusinessException exception = assertThrows(OrderBusinessException.class,
                () -> orderSubmissionService.submitOrder(dto));

        assertEquals(MessageConstant.PRODUCT_STOCK_NOT_ENOUGH, exception.getMessage());
        verify(orderDetailMapper, never()).insertBatch(any());
        verify(cartMapper, never()).deleteByUserId(any());
    }

    private void assignOrderId(Long orderId) {
        doAnswer(invocation -> {
            Orders order = invocation.getArgument(0);
            order.setId(orderId);
            return null;
        }).when(orderMapper).insert(any(Orders.class));
    }

    private Orders captureInsertedOrder() {
        ArgumentCaptor<Orders> captor = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).insert(captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private List<OrderDetail> captureOrderDetails() {
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(orderDetailMapper).insertBatch(captor.capture());
        return captor.getValue();
    }

    private static OrdersSubmitDTO submitDTO() {
        OrdersSubmitDTO dto = new OrdersSubmitDTO();
        dto.setAddressBookId(10L);
        dto.setPayMethod(1);
        dto.setAmount(new BigDecimal("120.00"));
        return dto;
    }

    private static ShippingAddress shippingAddress(Long memberId) {
        return ShippingAddress.builder()
                .memberId(memberId)
                .phone("0912345678")
                .consignee("Kevin")
                .cityName("台北市")
                .districtName("大安區")
                .detail("信義路一段1號")
                .build();
    }
}
