package com.sky.dto;

import lombok.Data;

import java.io.Serializable;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class JoinGroupBuyDTO implements Serializable {

    @NotBlank(message = "揪團編號不能為空")
    private String groupNo;
    @NotNull(message = "商品不能為空")
    private Long productId;
    @NotNull(message = "數量不能為空")
    @Min(value = 1, message = "數量必須大於 0")
    private Integer quantity;
    @NotNull(message = "地址不能為空")
    private Long addressId;
}
