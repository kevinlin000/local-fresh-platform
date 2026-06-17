package com.localfresh.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ProductPageQueryDTO implements Serializable {

    private int page;

    private int pageSize;

    private String productName;

    //分類id
    private Integer categoryId;

    //狀態 0表示停用 1表示啟用
    private Integer status;

    // 是否只查低庫存商品
    private Boolean lowStock;

}
