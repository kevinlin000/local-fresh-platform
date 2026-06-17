package com.localfresh.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class GiftBoxPageQueryDTO implements Serializable {

    private int page;

    private int pageSize;

    private String boxName;

    //分類id
    private Integer categoryId;

    //狀態 0表示停用 1表示啟用
    private Integer status;

}
