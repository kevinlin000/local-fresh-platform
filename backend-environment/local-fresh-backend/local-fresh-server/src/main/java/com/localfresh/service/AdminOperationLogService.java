package com.localfresh.service;

import com.localfresh.entity.Orders;

public interface AdminOperationLogService {

    void recordOrderAction(String action, Orders order, Integer afterStatus, String reason);

    void recordProductInventoryAdjustment(Long productId, Integer stockBefore, Integer stockAfter, String reason);
}
