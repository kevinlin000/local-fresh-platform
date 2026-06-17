package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.sky.constant.MessageConstant;
import com.sky.entity.Orders;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.OrderMapper;
import com.sky.service.OrderPaymentService;
import com.sky.service.payment.PaymentGateway;
import com.sky.service.support.OrderStatusTransitionPolicy;
import com.sky.vo.OrderPaymentVO;
import com.sky.websocket.WebSocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.sky.service.support.OrderStatusTransitionPolicy.Transition;

@Service
public class OrderPaymentServiceImpl implements OrderPaymentService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private WebSocketServer webSocketServer;

    @Autowired
    private PaymentGateway paymentGateway;

    @Override
    public OrderPaymentVO requestPayment(Orders order) {
        ensurePaymentSucceeded(order.getNumber());
        return paymentGateway.createPaymentRequest(order);
    }

    @Override
    public void handlePaymentSuccess(String orderNumber) {
        Orders ordersDB = orderMapper.getByNumber(orderNumber);
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (Orders.PAID.equals(ordersDB.getPayStatus())) {
            return;
        }
        if (!ensurePaymentSucceeded(orderNumber)) {
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", 1);
        payload.put("orderId", ordersDB.getId());
        payload.put("content", "訂單號：" + orderNumber);
        webSocketServer.sendToAllClient(JSON.toJSONString(payload));
    }

    private boolean ensurePaymentSucceeded(String orderNumber) {
        int updatedRows = orderMapper.markPaymentSucceededByNumber(orderNumber, Orders.PENDING_PAYMENT, Orders.UN_PAID,
                Orders.TO_BE_CONFIRMED, Orders.PAID, LocalDateTime.now());
        if (updatedRows > 0) {
            return true;
        }

        Orders latestOrder = orderMapper.getByNumber(orderNumber);
        if (latestOrder != null && Orders.PAID.equals(latestOrder.getPayStatus())) {
            return false;
        }
        OrderStatusTransitionPolicy.requireAllowed(latestOrder, Transition.PAY);
        throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
    }
}
