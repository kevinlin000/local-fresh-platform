package com.localfresh.service.impl;

import com.localfresh.constant.MessageConstant;
import com.localfresh.context.BaseContext;
import com.localfresh.dto.OrdersConfirmDTO;
import com.localfresh.entity.Orders;
import com.localfresh.exception.OrderBusinessException;
import com.localfresh.mapper.OrderMapper;
import com.localfresh.service.OrderFulfillmentService;
import com.localfresh.service.support.OrderStatusTransitionPolicy;
import com.localfresh.utils.JsonUtil;
import com.localfresh.websocket.WebSocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.localfresh.service.support.OrderStatusTransitionPolicy.Transition;

@Service
public class OrderFulfillmentServiceImpl implements OrderFulfillmentService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private WebSocketServer webSocketServer;

    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders ordersDB = orderMapper.getById(ordersConfirmDTO.getId());
        OrderStatusTransitionPolicy.requireAllowed(ordersDB, Transition.ADMIN_CONFIRM);

        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();

        orderMapper.update(orders);
    }

    @Override
    public void delivery(Long id) {
        Orders ordersDB = orderMapper.getById(id);
        OrderStatusTransitionPolicy.requireAllowed(ordersDB, Transition.START_DELIVERY);

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);

        orderMapper.update(orders);
    }

    @Override
    public void complete(Long id) {
        Orders ordersDB = orderMapper.getById(id);
        OrderStatusTransitionPolicy.requireAllowed(ordersDB, Transition.COMPLETE_DELIVERY);

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    @Override
    public void reminder(Long id) {
        Orders ordersDB = orderMapper.getById(id);
        if (ordersDB == null || !ordersDB.getUserId().equals(BaseContext.getCurrentId())) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("type", 2);
        map.put("orderId", id);
        map.put("content", "訂單號：" + ordersDB.getNumber());

        webSocketServer.sendToAllClient(JsonUtil.toJson(map));
    }
}
