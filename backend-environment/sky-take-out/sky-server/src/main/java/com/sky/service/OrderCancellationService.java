package com.sky.service;

import com.sky.entity.Orders;

public interface OrderCancellationService {

    void cancelOrder(Orders orders, String cancelReason, String rejectionReason,
                     String operatorType, Long operatorId) throws Exception;
}
