package com.localfresh.service.support;

import com.localfresh.constant.MessageConstant;
import com.localfresh.entity.Orders;
import com.localfresh.exception.OrderBusinessException;

import java.util.Set;

public final class OrderStatusTransitionPolicy {

    private OrderStatusTransitionPolicy() {
    }

    public enum Transition {
        PAY(Orders.TO_BE_CONFIRMED, Orders.PENDING_PAYMENT),
        USER_CANCEL(Orders.CANCELLED, Orders.PENDING_PAYMENT, Orders.TO_BE_CONFIRMED),
        ADMIN_CONFIRM(Orders.CONFIRMED, Orders.TO_BE_CONFIRMED),
        ADMIN_REJECT(Orders.CANCELLED, Orders.TO_BE_CONFIRMED),
        ADMIN_CANCEL(Orders.CANCELLED, Orders.PENDING_PAYMENT, Orders.TO_BE_CONFIRMED, Orders.CONFIRMED, Orders.DELIVERY_IN_PROGRESS),
        START_DELIVERY(Orders.DELIVERY_IN_PROGRESS, Orders.CONFIRMED),
        COMPLETE_DELIVERY(Orders.COMPLETED, Orders.DELIVERY_IN_PROGRESS);

        private final Integer targetStatus;
        private final Set<Integer> allowedSourceStatuses;

        Transition(Integer targetStatus, Integer... allowedSourceStatuses) {
            this.targetStatus = targetStatus;
            this.allowedSourceStatuses = Set.of(allowedSourceStatuses);
        }

        public Integer targetStatus() {
            return targetStatus;
        }

        public boolean allows(Integer sourceStatus) {
            return allowedSourceStatuses.contains(sourceStatus);
        }
    }

    public static void requireAllowed(Orders order, Transition transition) {
        if (order == null || !transition.allows(order.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
    }
}
