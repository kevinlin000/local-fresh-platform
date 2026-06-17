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

    // 訂單編號
    private String number;

    // 訂單狀態 1待付款 2待確認 3已確認 4配送中 5已完成 6已取消 8揪團中
    private Integer status;

    // 下單會員 id
    private Long userId;

    // 地址 id
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

    // 會員姓名
    private String userName;

    // 手機號碼
    private String phone;

    // 地址
    private String address;

    // 收件人
    private String consignee;

    private List<OrderDetail> orderDetails;

}
