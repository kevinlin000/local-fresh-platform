package com.localfresh.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 訂單概覽資料
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderOverViewVO implements Serializable {
    //待確認數量
    private Integer waitingOrders;

    //待配送數量
    private Integer deliveredOrders;

    //已完成數量
    private Integer completedOrders;

    //已取消數量
    private Integer cancelledOrders;

    //全部訂單
    private Integer allOrders;
}
