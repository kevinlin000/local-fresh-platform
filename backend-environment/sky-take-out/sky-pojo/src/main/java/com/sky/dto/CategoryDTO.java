package com.sky.dto;

import lombok.Data;

import java.io.Serializable;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class CategoryDTO implements Serializable {

    //主键
    private Long id;

    //类型 1 菜品分类 2 套餐分类
    @NotNull(message = "分類類型不能為空")
    @Min(value = 1, message = "分類類型錯誤")
    @Max(value = 2, message = "分類類型錯誤")
    private Integer type;

    //分类名称
    @NotBlank(message = "分類名稱不能為空")
    private String name;

    //排序
    @NotNull(message = "排序不能為空")
    @Min(value = 0, message = "排序不能小於 0")
    private Integer sort;

}
