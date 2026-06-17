package com.localfresh.dto;

import com.localfresh.entity.OrderDetail;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrdersDTO implements Serializable {

    private Long id;

    //订单号
    private String number;

    //订单状态 1待付款，2待派送，3已派送，4已完成，5已取消
    private Integer status;

    //下单用户id
    private Long userId;

    //地址id
    private Long addressBookId;

    // 下單時間
    private LocalDateTime orderTime;

    // 結帳時間
    private LocalDateTime checkoutTime;

    // 支付方式 1 Demo payment
    private Integer payMethod;

    // 實收金額
    private BigDecimal amount;

    // 備註
    private String remark;

    //用户名
    private String userName;

    //手机号
    private String phone;

    //地址
    private String address;

    //收货人
    private String consignee;

    private List<OrderDetail> orderDetails;

}
