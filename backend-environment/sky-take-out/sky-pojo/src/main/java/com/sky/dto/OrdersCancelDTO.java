package com.sky.dto;

import lombok.Data;

import java.io.Serializable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class OrdersCancelDTO implements Serializable {

    @NotNull(message = "訂單 id 不能為空")
    private Long id;
    //订单取消原因
    @NotBlank(message = "取消原因不能為空")
    private String cancelReason;

}
