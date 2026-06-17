package com.localfresh.service;

import com.localfresh.entity.Orders;
import com.localfresh.vo.OrderPaymentVO;

public interface OrderPaymentService {

    OrderPaymentVO requestPayment(Orders order);

    void handlePaymentSuccess(String orderNumber);
}
