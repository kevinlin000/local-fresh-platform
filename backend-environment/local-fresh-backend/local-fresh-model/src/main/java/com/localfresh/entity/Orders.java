package com.localfresh.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 訂單
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Orders implements Serializable {

    /**
     * 訂單狀態 1待付款 2待確認 3已確認 4配送中 5已完成 6已取消 8揪團中
     */
    public static final Integer PENDING_PAYMENT = 1;
    public static final Integer TO_BE_CONFIRMED = 2;
    public static final Integer CONFIRMED = 3;
    public static final Integer DELIVERY_IN_PROGRESS = 4;
    public static final Integer COMPLETED = 5;
    public static final Integer CANCELLED = 6;
    public static final Integer PENDING_GROUP = 8;

    /**
     * 支付狀態 0未支付 1已支付 2退款
     */
    public static final Integer UN_PAID = 0;
    public static final Integer PAID = 1;
    public static final Integer REFUND = 2;

    private static final long serialVersionUID = 1L;

    private Long id;

    // 訂單編號
    private String number;

    //訂單狀態 1待付款 2待確認 3已確認 4配送中 5已完成 6已取消 8揪團中
    private Integer status;

    //下單會員id
    private Long userId;

    // 地址 id
    private Long addressBookId;

    // 下單時間
    private LocalDateTime orderTime;

    // 結帳時間
    private LocalDateTime checkoutTime;

    // 支付方式 1 Demo payment / ECPay credit card
    private Integer payMethod;

    // 支付狀態 0 未支付 1 已支付 2 退款
    private Integer payStatus;

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

    // 訂單取消原因
    private String cancelReason;

    // 訂單拒絕原因
    private String rejectionReason;

    // 訂單取消時間
    private LocalDateTime cancelTime;

    // 預計送達時間
    private LocalDateTime estimatedDeliveryTime;

    // 配送狀態 1 立即送出 0 選擇指定時間
    private Integer deliveryStatus;

    // 送達時間
    private LocalDateTime deliveryTime;

    // 包裝費
    private int packAmount;

    // 餐具數量
    private int tablewareNumber;

    // 餐具數量狀態 1 按餐量提供 0 選擇指定數量
    private Integer tablewareStatus;
}
