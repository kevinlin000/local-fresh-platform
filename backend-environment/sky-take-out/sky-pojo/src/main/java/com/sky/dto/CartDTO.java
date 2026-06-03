package com.sky.dto;

import lombok.Data;
import java.io.Serializable;
import javax.validation.constraints.AssertTrue;

@Data
public class CartDTO implements Serializable {

    private Long productId;
    private Long giftBoxId;
    private String productSpec;

    @AssertTrue(message = "商品或直送箱不能為空")
    public boolean isItemSelected() {
        return productId != null || giftBoxId != null;
    }
}
