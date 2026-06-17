package com.localfresh.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaymentVO implements Serializable {

    private String nonceStr; // 隨機字串
    private String paySign; // 簽章
    private String timeStamp; // 時間戳
    private String signType; // 簽章演算法
    private String packageStr; // 付款請求識別值

}
