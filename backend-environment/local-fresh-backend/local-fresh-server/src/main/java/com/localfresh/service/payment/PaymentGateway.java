package com.localfresh.service.payment;

import com.localfresh.entity.Orders;
import com.localfresh.vo.OrderPaymentVO;

import java.util.Map;

public interface PaymentGateway {

    OrderPaymentVO createPaymentRequest(Orders order);

    void refund(Orders order, String reason);

    default String provider() {
        return "UNKNOWN";
    }

    default boolean completesPaymentOnRequest() {
        return false;
    }

    default PaymentCallbackCommand parsePaymentCallback(Map<String, String> payload) {
        throw new UnsupportedOperationException("Payment callback is not supported by this provider");
    }

    default PaymentQueryResult queryPaymentStatus(String orderNumber) {
        throw new UnsupportedOperationException("Payment status query is not supported by this provider");
    }
}
