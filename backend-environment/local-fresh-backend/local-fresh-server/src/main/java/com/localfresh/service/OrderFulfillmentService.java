package com.localfresh.service;

import com.localfresh.dto.OrdersConfirmDTO;

public interface OrderFulfillmentService {

    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    void delivery(Long id);

    void complete(Long id);

    void reminder(Long id);
}
