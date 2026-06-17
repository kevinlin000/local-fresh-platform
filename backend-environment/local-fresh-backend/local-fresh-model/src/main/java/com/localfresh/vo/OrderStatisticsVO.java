package com.localfresh.vo;

import lombok.Data;
import java.io.Serializable;

@Data
public class OrderStatisticsVO implements Serializable {
    //待確認數量
    private Integer toBeConfirmed;

    //待配送數量
    private Integer confirmed;

    //配送中數量
    private Integer deliveryInProgress;
}
