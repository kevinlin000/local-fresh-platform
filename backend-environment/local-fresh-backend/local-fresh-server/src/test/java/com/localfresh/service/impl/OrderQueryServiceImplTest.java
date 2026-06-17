package com.localfresh.service.impl;

import com.localfresh.constant.MessageConstant;
import com.localfresh.context.BaseContext;
import com.localfresh.entity.OrderDetail;
import com.localfresh.entity.Orders;
import com.localfresh.exception.OrderBusinessException;
import com.localfresh.mapper.OrderDetailMapper;
import com.localfresh.mapper.OrderMapper;
import com.localfresh.vo.OrderVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderQueryServiceImplTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderDetailMapper orderDetailMapper;

    @InjectMocks
    private OrderQueryServiceImpl orderQueryService;

    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
    }

    @Test
    void detailsShouldRejectMissingOrderWithoutQueryingDetails() {
        when(orderMapper.getById(99L)).thenReturn(null);

        OrderBusinessException exception = assertThrows(OrderBusinessException.class,
                () -> orderQueryService.details(99L));

        assertEquals(MessageConstant.ORDER_NOT_FOUND, exception.getMessage());
        verifyNoInteractions(orderDetailMapper);
    }

    @Test
    void userDetailsShouldRejectOrderOwnedByAnotherUser() {
        BaseContext.setCurrentId(100L);
        Orders order = new Orders();
        order.setId(10L);
        order.setUserId(200L);
        when(orderMapper.getById(10L)).thenReturn(order);

        OrderBusinessException exception = assertThrows(OrderBusinessException.class,
                () -> orderQueryService.userDetails(10L));

        assertEquals(MessageConstant.ORDER_NOT_FOUND, exception.getMessage());
        verifyNoInteractions(orderDetailMapper);
    }

    @Test
    void detailsShouldReturnOrderWithDetails() {
        Orders order = new Orders();
        order.setId(11L);
        order.setUserId(100L);
        when(orderMapper.getById(11L)).thenReturn(order);
        when(orderDetailMapper.getByOrderId(11L)).thenReturn(List.of(OrderDetail.builder()
                .name("高麗菜")
                .number(2)
                .build()));

        OrderVO orderVO = orderQueryService.details(11L);

        assertEquals(11L, orderVO.getId());
        assertEquals(1, orderVO.getOrderDetailList().size());
        assertEquals("高麗菜", orderVO.getOrderDetailList().get(0).getName());
    }
}
