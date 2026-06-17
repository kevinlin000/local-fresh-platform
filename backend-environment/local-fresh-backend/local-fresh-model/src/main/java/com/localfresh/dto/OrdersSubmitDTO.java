package com.localfresh.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data
public class OrdersSubmitDTO implements Serializable {
    //地址簿id
    @NotNull(message = "地址不能為空")
    private Long addressBookId;
    //付款方式
    @Min(value = 1, message = "付款方式錯誤")
    @Max(value = 2, message = "付款方式錯誤")
    private int payMethod;
    //备注
    private String remark;
    //预计送达时间
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime estimatedDeliveryTime;
    //配送状态  1立即送出  0选择具体时间
    @Min(value = 0, message = "配送狀態錯誤")
    @Max(value = 1, message = "配送狀態錯誤")
    private Integer deliveryStatus;
    //餐具数量
    @Min(value = 0, message = "餐具數量不能小於 0")
    private Integer tablewareNumber;
    //餐具数量状态  1按餐量提供  0选择具体数量
    @Min(value = 0, message = "餐具狀態錯誤")
    @Max(value = 1, message = "餐具狀態錯誤")
    private Integer tablewareStatus;
    //打包费
    @Min(value = 0, message = "打包費不能小於 0")
    private Integer packAmount;
    //总金额
    @NotNull(message = "訂單金額不能為空")
    @DecimalMin(value = "0.01", message = "訂單金額必須大於 0")
    private BigDecimal amount;
}
