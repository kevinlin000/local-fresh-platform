package com.localfresh.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 直送箱商品關係
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GiftBoxProduct implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    // 直送箱 id
    private Long giftBoxId;

    // 商品 id
    private Long productId;

    // 商品名稱（冗餘欄位）
    private String name;

    // 商品原價
    private BigDecimal price;

    // 份數
    private Integer copies;
}
