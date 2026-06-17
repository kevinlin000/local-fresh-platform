package com.localfresh.vo;

import com.localfresh.entity.OrderDetail;
import com.localfresh.entity.Orders;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderVO extends Orders implements Serializable {

    //訂單商品信息
    private String orderDishes;

    //訂單详情
    private List<OrderDetail> orderDetailList;

}
