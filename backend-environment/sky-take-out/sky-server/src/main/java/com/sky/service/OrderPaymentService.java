package com.sky.service;

import com.sky.entity.Orders;
import com.sky.vo.OrderPaymentVO;

public interface OrderPaymentService {

    OrderPaymentVO requestPayment(Orders order);

    void handlePaymentSuccess(String orderNumber);
}
