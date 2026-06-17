package com.localfresh.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 產地直送箱
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GiftBox implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    // 分類 id
    private Long categoryId;

    // 直送箱名稱
    private String boxName;

    // 直送箱價格
    private BigDecimal price;

    //狀態 0:停用 1:啟用
    private Integer status;

    // 描述資訊
    private String description;

    // 圖片
    private String image;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;
}
