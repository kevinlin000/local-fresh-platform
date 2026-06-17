package com.localfresh.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

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

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;

}
