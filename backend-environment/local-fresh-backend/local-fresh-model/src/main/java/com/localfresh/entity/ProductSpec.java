package com.localfresh.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 商品規格
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSpec implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    // 商品 id
    private Long productId;

    // 規格名稱
    private String name;

    //規格資料list
    private String value;

}
