package com.localfresh.dto;

import lombok.Data;

import java.io.Serializable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class OrdersRejectionDTO implements Serializable {

    @NotNull(message = "訂單 id 不能為空")
    private Long id;

    //訂單拒绝原因
    @NotBlank(message = "拒單原因不能為空")
    private String rejectionReason;

}
