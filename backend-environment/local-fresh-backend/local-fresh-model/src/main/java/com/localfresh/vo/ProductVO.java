package com.localfresh.vo;

import com.localfresh.entity.ProductSpec;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVO implements Serializable {

    private Long id;
    //商品名称
    private String productName;
    //商品分類id
    private Long categoryId;
    //商品价格
    private BigDecimal price;
    //图片
    private String image;
    //描述信息
    private String description;
    //0 停售 1 起售
    private Integer status;
    //可售庫存
    private Integer stock;
    //低庫存警示門檻
    private Integer lowStockThreshold;
    //更新時间
    private LocalDateTime updateTime;
    //分類名称
    private String categoryName;
    //商品關聯的規格
    private List<ProductSpec> productSpecs = new ArrayList<>();

    //private Integer copies;
}
