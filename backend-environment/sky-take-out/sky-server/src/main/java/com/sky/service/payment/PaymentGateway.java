package com.sky.service.payment;

import com.sky.entity.Orders;
import com.sky.vo.OrderPaymentVO;

public interface PaymentGateway {

    OrderPaymentVO createPaymentRequest(Orders order);

    void refund(Orders order, String reason);
}
