package com.localfresh.service;

import com.localfresh.dto.OrdersSubmitDTO;
import com.localfresh.vo.OrderSubmitVO;

public interface OrderSubmissionService {

    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);
}
