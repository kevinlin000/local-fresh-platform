package com.localfresh.dto;

import lombok.Data;

import java.io.Serializable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class CategoryDTO implements Serializable {

    // 主鍵
    private Long id;

    //類型 1 商品分類 2 直送箱分類
    @NotNull(message = "分類類型不能為空")
    @Min(value = 1, message = "分類類型錯誤")
    @Max(value = 2, message = "分類類型錯誤")
    private Integer type;

    // 分類名稱
    @NotBlank(message = "分類名稱不能為空")
    private String name;

    //排序
    @NotNull(message = "排序不能為空")
    @Min(value = 0, message = "排序不能小於 0")
    private Integer sort;

}
