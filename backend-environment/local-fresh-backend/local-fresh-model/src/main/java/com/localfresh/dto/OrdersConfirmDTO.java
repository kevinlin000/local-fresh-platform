package com.localfresh.dto;

import lombok.Data;

import java.io.Serializable;
import jakarta.validation.constraints.NotNull;

@Data
public class OrdersConfirmDTO implements Serializable {

    @NotNull(message = "訂單 id 不能為空")
    private Long id;
    //訂單狀態 1待付款 2待確認 3 已確認 4 配送中 5 已完成 6 已取消 7 退款
    private Integer status;

}
