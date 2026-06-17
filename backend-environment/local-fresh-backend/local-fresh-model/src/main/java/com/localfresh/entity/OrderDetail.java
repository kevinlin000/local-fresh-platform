package com.localfresh.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 訂單明細
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    // 名稱
    private String name;

    // 訂單 id
    private Long orderId;

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
}
