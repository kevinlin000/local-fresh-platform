package com.sky.service;

import com.sky.dto.OrdersConfirmDTO;

public interface OrderFulfillmentService {

    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    void delivery(Long id);

    void complete(Long id);

    void reminder(Long id);
}
