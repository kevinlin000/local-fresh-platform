package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.entity.Orders;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private OrderServiceImpl orderService;

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
    }

    @Test
    void detailsShouldRejectMissingOrderWithoutQueryingDetails() {
        when(orderMapper.getById(99L)).thenReturn(null);

        OrderBusinessException exception = assertThrows(OrderBusinessException.class,
                () -> orderService.details(99L));

        assertEquals(MessageConstant.ORDER_NOT_FOUND, exception.getMessage());
        verifyNoInteractions(orderDetailMapper);
    }

    private static Orders orderWithStatus(Long id, Integer status) {
        Orders order = new Orders();
        order.setId(id);
        order.setStatus(status);
        return order;
    }
}
