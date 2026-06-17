package com.localfresh.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 配送地址
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingAddress implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    // 會員 id
    private Long memberId;

    // 收件人
    private String consignee;

    // 手機號碼
    private String phone;

    // 性別 0 女 1 男
    private String sex;

    // 省/縣市代碼
    private String provinceCode;

    // 省/縣市名稱
    private String provinceName;

    // 城市代碼
    private String cityCode;

    // 城市名稱
    private String cityName;

    // 行政區代碼
    private String districtCode;

    // 行政區名稱
    private String districtName;

    // 詳細地址
    private String detail;

    // 標籤
    private String label;

    // 是否預設 0 否 1 是
    private Integer isDefault;
}
