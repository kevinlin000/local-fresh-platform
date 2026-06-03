package com.sky.dto;

import com.sky.entity.ProductSpec;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class ProductDTO implements Serializable {

    private Long id;
    //商品名称
    @NotBlank(message = "商品名稱不能為空")
    private String productName;
    //商品分类id
    @NotNull(message = "商品分類不能為空")
    private Long categoryId;
    //商品价格
    @NotNull(message = "商品價格不能為空")
    @DecimalMin(value = "0.01", message = "商品價格必須大於 0")
    private BigDecimal price;
    //图片
    private String image;
    //描述信息
    private String description;
    //0 停售 1 起售
    @NotNull(message = "商品狀態不能為空")
    @Min(value = 0, message = "商品狀態錯誤")
    @Max(value = 1, message = "商品狀態錯誤")
    private Integer status;
    //可售庫存
    @Min(value = 0, message = "商品庫存不能小於 0")
    private Integer stock;
    //低庫存警示門檻
    @Min(value = 0, message = "低庫存門檻不能小於 0")
    private Integer lowStockThreshold;
    //商品规格
    private List<ProductSpec> productSpecs = new ArrayList<>();

}
