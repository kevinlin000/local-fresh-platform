package com.localfresh.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 直送箱總览
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GiftBoxOverViewVO implements Serializable {
    // 已上架數量
    private Integer sold;

    // 已下架數量
    private Integer discontinued;
}
