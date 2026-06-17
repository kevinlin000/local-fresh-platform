package com.localfresh.dto;

import lombok.Data;

import java.io.Serializable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data
public class InitiateGroupBuyDTO implements Serializable {

    @NotNull(message = "商品不能為空")
    private Long productId;
    @NotNull(message = "數量不能為空")
    @Min(value = 1, message = "數量必須大於 0")
    private Integer quantity;
    @NotNull(message = "地址不能為空")
    private Long addressId;
    @NotNull(message = "成團人數不能為空")
    @Min(value = 2, message = "成團人數至少為 2")
    @Max(value = 100, message = "成團人數不能超過 100")
    private Integer requiredCount;
}
