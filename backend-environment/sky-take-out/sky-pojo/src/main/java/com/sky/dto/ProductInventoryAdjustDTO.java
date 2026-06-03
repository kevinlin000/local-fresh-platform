package com.sky.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class ProductInventoryAdjustDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "庫存異動量不能為空")
    private Integer changeQuantity;

    @NotBlank(message = "庫存調整原因不能為空")
    @Size(max = 120, message = "庫存調整原因不能超過 120 字")
    private String reason;
}
