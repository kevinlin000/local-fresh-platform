package com.localfresh.service.support;

import com.localfresh.entity.Orders;
import com.localfresh.exception.OrderBusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.localfresh.service.support.OrderStatusTransitionPolicy.Transition;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderStatusTransitionPolicyTest {

    private static final List<Integer> ORDER_STATUSES = List.of(
            Orders.PENDING_PAYMENT,
            Orders.TO_BE_CONFIRMED,
            Orders.CONFIRMED,
            Orders.DELIVERY_IN_PROGRESS,
            Orders.COMPLETED,
            Orders.CANCELLED,
            Orders.PENDING_GROUP
    );

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

    @Test
    void shouldDefineExpectedTargetStatusForEachTransition() {
        assertEquals(Orders.TO_BE_CONFIRMED, Transition.PAY.targetStatus());
        assertEquals(Orders.CANCELLED, Transition.USER_CANCEL.targetStatus());
        assertEquals(Orders.CONFIRMED, Transition.ADMIN_CONFIRM.targetStatus());
        assertEquals(Orders.CANCELLED, Transition.ADMIN_REJECT.targetStatus());
        assertEquals(Orders.CANCELLED, Transition.ADMIN_CANCEL.targetStatus());
        assertEquals(Orders.DELIVERY_IN_PROGRESS, Transition.START_DELIVERY.targetStatus());
        assertEquals(Orders.COMPLETED, Transition.COMPLETE_DELIVERY.targetStatus());
    }

    @Test
    void shouldAllowOnlyDocumentedSourcesForEachTransition() {
        assertAllowedSources(Transition.PAY, Orders.PENDING_PAYMENT);
        assertAllowedSources(Transition.USER_CANCEL, Orders.PENDING_PAYMENT, Orders.TO_BE_CONFIRMED);
        assertAllowedSources(Transition.ADMIN_CONFIRM, Orders.TO_BE_CONFIRMED);
        assertAllowedSources(Transition.ADMIN_REJECT, Orders.TO_BE_CONFIRMED);
        assertAllowedSources(Transition.ADMIN_CANCEL, Orders.PENDING_PAYMENT, Orders.TO_BE_CONFIRMED,
                Orders.CONFIRMED, Orders.DELIVERY_IN_PROGRESS);
        assertAllowedSources(Transition.START_DELIVERY, Orders.CONFIRMED);
        assertAllowedSources(Transition.COMPLETE_DELIVERY, Orders.DELIVERY_IN_PROGRESS);
    }

    @Test
    void shouldRejectMissingOrderForAnyTransition() {
        for (Transition transition : Transition.values()) {
            assertThrows(OrderBusinessException.class,
                    () -> OrderStatusTransitionPolicy.requireAllowed(null, transition));
        }
    }

    private static void assertAllows(Integer status, Transition transition) {
        assertDoesNotThrow(() -> OrderStatusTransitionPolicy.requireAllowed(orderWithStatus(status), transition));
    }

    private static void assertRejects(Integer status, Transition transition) {
        assertThrows(OrderBusinessException.class,
                () -> OrderStatusTransitionPolicy.requireAllowed(orderWithStatus(status), transition));
    }

    private static void assertAllowedSources(Transition transition, Integer... allowedStatuses) {
        List<Integer> allowed = List.of(allowedStatuses);

        for (Integer status : ORDER_STATUSES) {
            if (allowed.contains(status)) {
                assertAllows(status, transition);
            } else {
                assertRejects(status, transition);
            }
        }
    }

    private static Orders orderWithStatus(Integer status) {
        Orders order = new Orders();
        order.setStatus(status);
        return order;
    }
}
