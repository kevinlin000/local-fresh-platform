package com.sky.dto;

import lombok.Data;

import java.io.Serializable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class OrdersRejectionDTO implements Serializable {

    @NotNull(message = "訂單 id 不能為空")
    private Long id;

    //订单拒绝原因
    @NotBlank(message = "拒單原因不能為空")
    private String rejectionReason;

}
