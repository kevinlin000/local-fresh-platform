package com.sky.dto;

import com.sky.entity.GiftBoxProduct;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class GiftBoxDTO implements Serializable {

    private Long id;

    //分类id
    @NotNull(message = "直送箱分類不能為空")
    private Long categoryId;

    //直送箱名称
    @NotBlank(message = "直送箱名稱不能為空")
    private String boxName;

    //直送箱价格
    @NotNull(message = "直送箱價格不能為空")
    @DecimalMin(value = "0.01", message = "直送箱價格必須大於 0")
    private BigDecimal price;

    //状态 0:停用 1:启用
    @NotNull(message = "直送箱狀態不能為空")
    @Min(value = 0, message = "直送箱狀態錯誤")
    @Max(value = 1, message = "直送箱狀態錯誤")
    private Integer status;

    //描述信息
    private String description;

    //图片
    private String image;

    //直送箱单品关系
    private List<GiftBoxProduct> giftBoxProducts = new ArrayList<>();

}
