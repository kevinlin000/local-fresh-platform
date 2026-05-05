package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class InitiateGroupBuyDTO implements Serializable {

    private Long productId;
    private Integer quantity;
    private Long addressId;
    private Integer requiredCount;
}
