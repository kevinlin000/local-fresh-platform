package com.localfresh.dto;

import lombok.Data;
import java.io.Serializable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class OrdersPaymentDTO implements Serializable {
    //訂單号
    @NotBlank(message = "訂單號不能為空")
    private String orderNumber;

    //付款方式
    @NotNull(message = "付款方式不能為空")
    @Min(value = 1, message = "付款方式錯誤")
    @Max(value = 2, message = "付款方式錯誤")
    private Integer payMethod;

}
