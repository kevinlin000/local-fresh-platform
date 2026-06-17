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

    //分类id
    private Long categoryId;

    //直送箱名称
    private String boxName;

    //直送箱价格
    private BigDecimal price;

    //状态 0:停用 1:启用
    private Integer status;

    //描述信息
    private String description;

    //图片
    private String image;

    //更新时间
    private LocalDateTime updateTime;

    //分类名称
    private String categoryName;

    //直送箱和商品的关联关系
    private List<GiftBoxProduct> giftBoxProducts = new ArrayList<>();
}
