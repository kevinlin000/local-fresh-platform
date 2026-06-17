package com.localfresh.service;

import com.localfresh.dto.OrdersPageQueryDTO;
import com.localfresh.result.PageResult;
import com.localfresh.vo.OrderStatisticsVO;
import com.localfresh.vo.OrderVO;

public interface OrderQueryService {

    PageResult pageQuery4User(int pageNum, int pageSize, Integer status);

    OrderVO details(Long id);

    OrderVO userDetails(Long id);

    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    OrderStatisticsVO statistics();
}
