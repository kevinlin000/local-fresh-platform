package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class JoinGroupBuyDTO implements Serializable {

    private String groupNo;
    private Long productId;
    private Integer quantity;
    private Long addressId;
}
