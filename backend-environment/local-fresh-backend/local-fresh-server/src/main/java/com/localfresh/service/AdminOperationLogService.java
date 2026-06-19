package com.localfresh.service;

import com.localfresh.dto.AdminOperationLogPageQueryDTO;
import com.localfresh.entity.Orders;
import com.localfresh.result.PageResult;

public interface AdminOperationLogService {

    void recordOrderAction(String action, Orders order, Integer afterStatus, String reason);

    void recordProductInventoryAdjustment(Long productId, Integer stockBefore, Integer stockAfter, String reason);

    PageResult pageQuery(AdminOperationLogPageQueryDTO queryDTO);
}
