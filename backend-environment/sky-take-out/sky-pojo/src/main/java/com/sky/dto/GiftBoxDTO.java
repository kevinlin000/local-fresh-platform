package com.sky.dto;

import com.sky.entity.GiftBoxProduct;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class GiftBoxDTO implements Serializable {

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

    //直送箱单品关系
    private List<GiftBoxProduct> giftBoxProducts = new ArrayList<>();

}
