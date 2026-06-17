package com.localfresh.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 購物車
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cart implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    // 名稱
    private String name;

    // 會員 id
    private Long userId;

    // 商品 id
    private Long productId;

    // 直送箱 id
    private Long giftBoxId;

    //商品規格
    private String productSpec;

    //數量
    private Integer number;

    //金額
    private BigDecimal amount;

    // 圖片
    private String image;

    private LocalDateTime createTime;
}
