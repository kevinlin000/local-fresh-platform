package com.sky.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class CartDTO implements Serializable {

    private Long productId;
    private Long giftBoxId;
    private String productSpec;

}
