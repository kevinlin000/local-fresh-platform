package com.localfresh.service.payment;

import com.localfresh.entity.Orders;
import com.localfresh.vo.OrderPaymentVO;

public interface PaymentGateway {

    OrderPaymentVO createPaymentRequest(Orders order);

    void refund(Orders order, String reason);

    default boolean completesPaymentOnRequest() {
        return false;
    }
}
