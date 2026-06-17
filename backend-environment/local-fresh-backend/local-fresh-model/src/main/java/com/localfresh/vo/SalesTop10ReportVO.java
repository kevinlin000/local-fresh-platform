package com.localfresh.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesTop10ReportVO implements Serializable {

    // 商品名稱列表，以逗號分隔，例如：有機A菜,宜蘭三星蔥,池上越光米
    private String nameList;

    // 銷量列表，以逗號分隔，例如：260,215,200
    private String numberList;

}
