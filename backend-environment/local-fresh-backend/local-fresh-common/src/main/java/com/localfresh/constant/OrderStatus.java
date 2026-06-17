package com.localfresh.constant;

public final class OrderStatus {

    private OrderStatus() {
    }

    public static final Integer PENDING_PAYMENT = 1;
    public static final Integer TO_BE_CONFIRMED = 2;
    public static final Integer CONFIRMED = 3;
    public static final Integer DELIVERY_IN_PROGRESS = 4;
    public static final Integer COMPLETED = 5;
    public static final Integer CANCELLED = 6;
    public static final Integer REFUND = 7;
    public static final Integer PENDING_GROUP = 8;
}
