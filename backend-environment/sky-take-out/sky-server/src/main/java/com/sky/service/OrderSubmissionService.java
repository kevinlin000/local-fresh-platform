package com.sky.service;

import com.sky.dto.OrdersSubmitDTO;
import com.sky.vo.OrderSubmitVO;

public interface OrderSubmissionService {

    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);
}
