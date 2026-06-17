package com.localfresh.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSubmitVO implements Serializable {
    //訂單id
    private Long id;
    //訂單号
    private String orderNumber;
    //訂單金額
    private BigDecimal orderAmount;
    //下單時间
    private LocalDateTime orderTime;
}
