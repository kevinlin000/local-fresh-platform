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
    // 商品名稱
    private String productName;
    // 商品分類 id
    private Long categoryId;
    // 商品價格
    private BigDecimal price;
    // 圖片
    private String image;
    // 描述資訊
    private String description;
    //0 下架 1 上架
    private Integer status;
    //可售庫存
    private Integer stock;
    //低庫存警示門檻
    private Integer lowStockThreshold;
    // 更新時間
    private LocalDateTime updateTime;
    // 分類名稱
    private String categoryName;
    //商品關聯的規格
    private List<ProductSpec> productSpecs = new ArrayList<>();

    //private Integer copies;
}
