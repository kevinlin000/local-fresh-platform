package com.localfresh.service.support;

import com.localfresh.entity.Orders;
import com.localfresh.exception.OrderBusinessException;
import org.junit.jupiter.api.Test;

import static com.localfresh.service.support.OrderStatusTransitionPolicy.Transition;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderStatusTransitionPolicyTest {

    @Test
    void shouldAllowNormalOrderLifecycleTransitions() {
        assertAllows(Orders.PENDING_PAYMENT, Transition.PAY);
        assertAllows(Orders.TO_BE_CONFIRMED, Transition.ADMIN_CONFIRM);
        assertAllows(Orders.CONFIRMED, Transition.START_DELIVERY);
        assertAllows(Orders.DELIVERY_IN_PROGRESS, Transition.COMPLETE_DELIVERY);
    }

    @Test
    void shouldAllowOnlyCancelableStatusesForUserCancel() {
        assertAllows(Orders.PENDING_PAYMENT, Transition.USER_CANCEL);
        assertAllows(Orders.TO_BE_CONFIRMED, Transition.USER_CANCEL);

        assertRejects(Orders.CONFIRMED, Transition.USER_CANCEL);
        assertRejects(Orders.DELIVERY_IN_PROGRESS, Transition.USER_CANCEL);
        assertRejects(Orders.COMPLETED, Transition.USER_CANCEL);
        assertRejects(Orders.CANCELLED, Transition.USER_CANCEL);
        assertRejects(Orders.PENDING_GROUP, Transition.USER_CANCEL);
    }

    @Test
    void shouldRejectAdminConfirmWhenOrderIsNotWaitingForAcceptance() {
        assertRejects(Orders.PENDING_PAYMENT, Transition.ADMIN_CONFIRM);
        assertRejects(Orders.CONFIRMED, Transition.ADMIN_CONFIRM);
        assertRejects(Orders.CANCELLED, Transition.ADMIN_CONFIRM);
    }

    @Test
    void shouldRejectTerminalOrdersForAdminCancel() {
        assertAllows(Orders.PENDING_PAYMENT, Transition.ADMIN_CANCEL);
        assertAllows(Orders.TO_BE_CONFIRMED, Transition.ADMIN_CANCEL);
        assertAllows(Orders.CONFIRMED, Transition.ADMIN_CANCEL);
        assertAllows(Orders.DELIVERY_IN_PROGRESS, Transition.ADMIN_CANCEL);

        assertRejects(Orders.COMPLETED, Transition.ADMIN_CANCEL);
        assertRejects(Orders.CANCELLED, Transition.ADMIN_CANCEL);
        assertRejects(Orders.PENDING_GROUP, Transition.ADMIN_CANCEL);
    }

    private static void assertAllows(Integer status, Transition transition) {
        assertDoesNotThrow(() -> OrderStatusTransitionPolicy.requireAllowed(orderWithStatus(status), transition));
    }

    private static void assertRejects(Integer status, Transition transition) {
        assertThrows(OrderBusinessException.class,
                () -> OrderStatusTransitionPolicy.requireAllowed(orderWithStatus(status), transition));
    }

    private static Orders orderWithStatus(Integer status) {
        Orders order = new Orders();
        order.setStatus(status);
        return order;
    }
}
