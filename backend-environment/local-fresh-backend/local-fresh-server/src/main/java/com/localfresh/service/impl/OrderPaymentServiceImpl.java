package com.localfresh.service.impl;

import com.localfresh.constant.MessageConstant;
import com.localfresh.entity.Orders;
import com.localfresh.entity.PaymentEvent;
import com.localfresh.exception.OrderBusinessException;
import com.localfresh.mapper.OrderMapper;
import com.localfresh.mapper.PaymentEventMapper;
import com.localfresh.service.OrderPaymentService;
import com.localfresh.service.payment.PaymentGateway;
import com.localfresh.service.support.OrderStatusTransitionPolicy;
import com.localfresh.utils.JsonUtil;
import com.localfresh.vo.OrderPaymentVO;
import com.localfresh.websocket.WebSocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.localfresh.service.support.OrderStatusTransitionPolicy.Transition;

@Service
public class OrderPaymentServiceImpl implements OrderPaymentService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private WebSocketServer webSocketServer;

    @Autowired
    private PaymentGateway paymentGateway;

    @Autowired
    private PaymentEventMapper paymentEventMapper;

    @Override
    public OrderPaymentVO requestPayment(Orders order) {
        OrderPaymentVO paymentRequest = paymentGateway.createPaymentRequest(order);
        recordPaymentEvent(order, PaymentEvent.EVENT_REQUEST_CREATED, PaymentEvent.RESULT_PENDING,
                paymentRequest.getPackageStr(), JsonUtil.toJson(paymentRequest));
        if (paymentGateway.completesPaymentOnRequest()) {
            handlePaymentSuccess(order.getNumber());
        }
        return paymentRequest;
    }

    @Override
    public void handlePaymentSuccess(String orderNumber) {
        Orders ordersDB = orderMapper.getByNumber(orderNumber);
        if (ordersDB == null) {
            recordPaymentEvent(null, orderNumber, PaymentEvent.EVENT_CALLBACK_REJECTED, PaymentEvent.RESULT_REJECTED,
                    null, null);
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (Orders.PAID.equals(ordersDB.getPayStatus())) {
            recordPaymentEvent(ordersDB, PaymentEvent.EVENT_CALLBACK_DUPLICATE, PaymentEvent.RESULT_IGNORED,
                    null, null);
            return;
        }
        try {
            if (!ensurePaymentSucceeded(orderNumber)) {
                recordPaymentEvent(ordersDB, PaymentEvent.EVENT_CALLBACK_DUPLICATE, PaymentEvent.RESULT_IGNORED,
                        null, null);
                return;
            }
        } catch (OrderBusinessException ex) {
            recordPaymentEvent(ordersDB, PaymentEvent.EVENT_CALLBACK_REJECTED, PaymentEvent.RESULT_REJECTED,
                    null, null);
            throw ex;
        }
        recordPaymentEvent(ordersDB, PaymentEvent.EVENT_CALLBACK_SUCCEEDED, PaymentEvent.RESULT_SUCCEEDED, null, null);

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", 1);
        payload.put("orderId", ordersDB.getId());
        payload.put("content", "訂單號：" + orderNumber);
        webSocketServer.sendToAllClient(JsonUtil.toJson(payload));
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

    private void recordPaymentEvent(Orders order, String eventType, String result, String providerReference,
                                    String rawPayload) {
        recordPaymentEvent(order, order.getNumber(), eventType, result, providerReference, rawPayload);
    }

    private void recordPaymentEvent(Orders order, String orderNumber, String eventType, String result,
                                    String providerReference, String rawPayload) {
        PaymentEvent paymentEvent = PaymentEvent.builder()
                .orderId(order == null ? null : order.getId())
                .orderNumber(orderNumber)
                .provider(resolveProvider())
                .eventType(eventType)
                .providerReference(providerReference)
                .amount(order == null ? null : order.getAmount())
                .result(result)
                .rawPayload(rawPayload)
                .createdAt(LocalDateTime.now())
                .build();
        paymentEventMapper.insert(paymentEvent);
    }

    private String resolveProvider() {
        String provider = paymentGateway.provider();
        if (provider == null || provider.isBlank()) {
            return PaymentEvent.PROVIDER_UNKNOWN;
        }
        return provider;
    }
}
