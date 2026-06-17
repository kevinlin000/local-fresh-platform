package com.localfresh.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductItemVO implements Serializable {

    // 商品名稱
    private String name;

    // 份數
    private Integer copies;

    // 商品圖片
    private String image;

    // 商品描述
    private String description;
}
