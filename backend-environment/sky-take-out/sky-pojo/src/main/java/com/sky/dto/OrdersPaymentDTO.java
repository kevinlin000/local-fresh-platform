package com.sky.dto;

import lombok.Data;
import java.io.Serializable;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class OrdersPaymentDTO implements Serializable {
    //订单号
    @NotBlank(message = "訂單號不能為空")
    private String orderNumber;

    //付款方式
    @NotNull(message = "付款方式不能為空")
    @Min(value = 1, message = "付款方式錯誤")
    @Max(value = 2, message = "付款方式錯誤")
    private Integer payMethod;

}
