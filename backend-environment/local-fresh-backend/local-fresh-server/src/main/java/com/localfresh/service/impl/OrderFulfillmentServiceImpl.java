package com.localfresh.service.impl;

import com.localfresh.constant.MessageConstant;
import com.localfresh.context.BaseContext;
import com.localfresh.dto.OrdersConfirmDTO;
import com.localfresh.entity.Orders;
import com.localfresh.exception.OrderBusinessException;
import com.localfresh.mapper.OrderMapper;
import com.localfresh.service.AdminOperationLogService;
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

    private static final String ACTION_ORDER_CONFIRM = "ORDER_CONFIRM";
    private static final String ACTION_ORDER_START_DELIVERY = "ORDER_START_DELIVERY";
    private static final String ACTION_ORDER_COMPLETE = "ORDER_COMPLETE";

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private WebSocketServer webSocketServer;

    @Autowired
    private AdminOperationLogService adminOperationLogService;

    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders ordersDB = orderMapper.getById(ordersConfirmDTO.getId());
        OrderStatusTransitionPolicy.requireAllowed(ordersDB, Transition.ADMIN_CONFIRM);

        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();

        orderMapper.update(orders);
        adminOperationLogService.recordOrderAction(ACTION_ORDER_CONFIRM, ordersDB, Orders.CONFIRMED, null);
    }

    @Override
    public void delivery(Long id) {
        Orders ordersDB = orderMapper.getById(id);
        OrderStatusTransitionPolicy.requireAllowed(ordersDB, Transition.START_DELIVERY);

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);

        orderMapper.update(orders);
        adminOperationLogService.recordOrderAction(ACTION_ORDER_START_DELIVERY, ordersDB,
                Orders.DELIVERY_IN_PROGRESS, null);
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
        adminOperationLogService.recordOrderAction(ACTION_ORDER_COMPLETE, ordersDB, Orders.COMPLETED, null);
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
