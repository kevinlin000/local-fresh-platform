package com.sky.service;

import com.sky.dto.OrdersPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;

public interface OrderQueryService {

    PageResult pageQuery4User(int pageNum, int pageSize, Integer status);

    OrderVO details(Long id);

    OrderVO userDetails(Long id);

    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    OrderStatisticsVO statistics();
}
