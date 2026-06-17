package com.localfresh.vo;

import com.localfresh.entity.GiftBoxProduct;
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
public class GiftBoxVO implements Serializable {

    private Long id;

    // 分類 id
    private Long categoryId;

    // 直送箱名稱
    private String boxName;

    // 直送箱價格
    private BigDecimal price;

    //狀態 0:停用 1:啟用
    private Integer status;

    // 描述資訊
    private String description;

    // 圖片
    private String image;

    // 更新時間
    private LocalDateTime updateTime;

    // 分類名稱
    private String categoryName;

    //直送箱和商品的關聯關係
    private List<GiftBoxProduct> giftBoxProducts = new ArrayList<>();
}
