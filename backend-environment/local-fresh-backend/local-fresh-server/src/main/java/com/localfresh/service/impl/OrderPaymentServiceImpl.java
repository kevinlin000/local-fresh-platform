package com.localfresh.service.impl;

import com.localfresh.constant.MessageConstant;
import com.localfresh.entity.Orders;
import com.localfresh.entity.PaymentEvent;
import com.localfresh.exception.OrderBusinessException;
import com.localfresh.mapper.OrderMapper;
import com.localfresh.mapper.PaymentEventMapper;
import com.localfresh.service.BusinessMetricsService;
import com.localfresh.service.OrderPaymentService;
import com.localfresh.service.payment.PaymentCallbackCommand;
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

    @Autowired
    private BusinessMetricsService businessMetricsService;

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
        handlePaymentCallback(PaymentCallbackCommand.builder()
                .provider(resolveProvider())
                .orderNumber(orderNumber)
                .paymentSucceeded(true)
                .build());
    }

    @Override
    public void handlePaymentCallback(PaymentCallbackCommand callbackCommand) {
        if (!callbackCommand.isPaymentSucceeded()) {
            recordPaymentEvent(null, callbackCommand.getOrderNumber(), callbackCommand.getProvider(),
                    PaymentEvent.EVENT_CALLBACK_REJECTED, PaymentEvent.RESULT_REJECTED,
                    callbackCommand.getProviderReference(), callbackCommand.getProviderTradeNo(),
                    callbackCommand.getRawPayload());
            businessMetricsService.recordPaymentCallback(callbackCommand.getProvider(), PaymentEvent.RESULT_REJECTED);
            throw new OrderBusinessException(MessageConstant.PAYMENT_CALLBACK_FAILED);
        }
        handlePaymentSuccess(callbackCommand);
    }

    private void handlePaymentSuccess(PaymentCallbackCommand callbackCommand) {
        String orderNumber = callbackCommand.getOrderNumber();
        Orders ordersDB = orderMapper.getByNumber(orderNumber);
        if (ordersDB == null) {
            recordPaymentEvent(null, orderNumber, callbackCommand.getProvider(),
                    PaymentEvent.EVENT_CALLBACK_REJECTED, PaymentEvent.RESULT_REJECTED,
                    callbackCommand.getProviderReference(), callbackCommand.getProviderTradeNo(),
                    callbackCommand.getRawPayload());
            businessMetricsService.recordPaymentCallback(callbackCommand.getProvider(), PaymentEvent.RESULT_REJECTED);
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (Orders.PAID.equals(ordersDB.getPayStatus())) {
            recordPaymentEvent(ordersDB, callbackCommand.getProvider(), PaymentEvent.EVENT_CALLBACK_DUPLICATE,
                    PaymentEvent.RESULT_IGNORED, callbackCommand.getProviderReference(),
                    callbackCommand.getProviderTradeNo(), callbackCommand.getRawPayload());
            businessMetricsService.recordPaymentCallback(callbackCommand.getProvider(), PaymentEvent.RESULT_IGNORED);
            return;
        }
        try {
            if (!ensurePaymentSucceeded(orderNumber)) {
                recordPaymentEvent(ordersDB, callbackCommand.getProvider(), PaymentEvent.EVENT_CALLBACK_DUPLICATE,
                        PaymentEvent.RESULT_IGNORED, callbackCommand.getProviderReference(),
                        callbackCommand.getProviderTradeNo(), callbackCommand.getRawPayload());
                businessMetricsService.recordPaymentCallback(callbackCommand.getProvider(), PaymentEvent.RESULT_IGNORED);
                return;
            }
        } catch (OrderBusinessException ex) {
            recordPaymentEvent(ordersDB, callbackCommand.getProvider(), PaymentEvent.EVENT_CALLBACK_REJECTED,
                    PaymentEvent.RESULT_REJECTED, callbackCommand.getProviderReference(),
                    callbackCommand.getProviderTradeNo(), callbackCommand.getRawPayload());
            businessMetricsService.recordPaymentCallback(callbackCommand.getProvider(), PaymentEvent.RESULT_REJECTED);
            throw ex;
        }
        recordPaymentEvent(ordersDB, callbackCommand.getProvider(), PaymentEvent.EVENT_CALLBACK_SUCCEEDED,
                PaymentEvent.RESULT_SUCCEEDED, callbackCommand.getProviderReference(),
                callbackCommand.getProviderTradeNo(), callbackCommand.getRawPayload());
        businessMetricsService.recordPaymentCallback(callbackCommand.getProvider(), PaymentEvent.RESULT_SUCCEEDED);

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
        recordPaymentEvent(order, order.getNumber(), resolveProvider(), eventType, result, providerReference, null,
                rawPayload);
    }

    private void recordPaymentEvent(Orders order, String orderNumber, String eventType, String result,
                                    String providerReference, String rawPayload) {
        recordPaymentEvent(order, orderNumber, resolveProvider(), eventType, result, providerReference, null,
                rawPayload);
    }

    private void recordPaymentEvent(Orders order, String provider, String eventType, String result,
                                    String providerReference, String providerTradeNo, String rawPayload) {
        recordPaymentEvent(order, order.getNumber(), provider, eventType, result, providerReference, providerTradeNo,
                rawPayload);
    }

    private void recordPaymentEvent(Orders order, String orderNumber, String provider, String eventType, String result,
                                    String providerReference, String providerTradeNo, String rawPayload) {
        String resolvedProvider = normalizeProvider(provider);
        PaymentEvent paymentEvent = PaymentEvent.builder()
                .orderId(order == null ? null : order.getId())
                .orderNumber(orderNumber)
                .provider(resolvedProvider)
                .eventType(eventType)
                .providerReference(providerReference)
                .providerTradeNo(providerTradeNo)
                .idempotencyKey(buildIdempotencyKey(resolvedProvider, eventType, orderNumber, providerReference,
                        providerTradeNo))
                .amount(order == null ? null : order.getAmount())
                .result(result)
                .rawPayload(rawPayload)
                .createdAt(LocalDateTime.now())
                .build();
        paymentEventMapper.insert(paymentEvent);
    }

    private String buildIdempotencyKey(String provider, String eventType, String orderNumber, String providerReference,
                                       String providerTradeNo) {
        if (providerTradeNo != null && !providerTradeNo.isBlank()) {
            return String.join(":", provider, eventType, orderNumber, providerTradeNo);
        }
        if (providerReference != null && !providerReference.isBlank()) {
            return String.join(":", provider, eventType, orderNumber, providerReference);
        }
        return String.join(":", provider, eventType, orderNumber);
    }

    private String resolveProvider() {
        return normalizeProvider(paymentGateway.provider());
    }

    private String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return PaymentEvent.PROVIDER_UNKNOWN;
        }
        return provider;
    }
}
