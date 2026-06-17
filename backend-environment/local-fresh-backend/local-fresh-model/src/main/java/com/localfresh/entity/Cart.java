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

    //名称
    private String name;

    //会员id
    private Long userId;

    //商品id
    private Long productId;

    //直送箱id
    private Long giftBoxId;

    //商品規格
    private String productSpec;

    //數量
    private Integer number;

    //金額
    private BigDecimal amount;

    //图片
    private String image;

    private LocalDateTime createTime;
}
