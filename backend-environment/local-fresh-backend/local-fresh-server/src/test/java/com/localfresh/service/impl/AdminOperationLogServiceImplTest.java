package com.localfresh.service.impl;

import com.localfresh.context.BaseContext;
import com.localfresh.entity.AdminOperationLog;
import com.localfresh.entity.Orders;
import com.localfresh.mapper.AdminOperationLogMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminOperationLogServiceImplTest {

    @Mock
    private AdminOperationLogMapper adminOperationLogMapper;

    @InjectMocks
    private AdminOperationLogServiceImpl adminOperationLogService;

    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
    }

    @Test
    void recordOrderActionShouldPersistAdminAuditEntry() {
        BaseContext.setCurrentId(88L);
        Orders order = new Orders();
        order.setId(10L);
        order.setStatus(Orders.TO_BE_CONFIRMED);

        adminOperationLogService.recordOrderAction("ORDER_CONFIRM", order, Orders.CONFIRMED, null);

        ArgumentCaptor<AdminOperationLog> captor = ArgumentCaptor.forClass(AdminOperationLog.class);
        verify(adminOperationLogMapper).insert(captor.capture());
        AdminOperationLog log = captor.getValue();
        assertEquals("ORDER_CONFIRM", log.getAction());
        assertEquals("ORDER", log.getTargetType());
        assertEquals(10L, log.getTargetId());
        assertEquals(String.valueOf(Orders.TO_BE_CONFIRMED), log.getBeforeValue());
        assertEquals(String.valueOf(Orders.CONFIRMED), log.getAfterValue());
        assertEquals("ADMIN", log.getOperatorType());
        assertEquals(88L, log.getOperatorId());
        assertNotNull(log.getCreatedAt());
    }

    @Test
    void recordProductInventoryAdjustmentShouldPersistStockChangeAuditEntry() {
        BaseContext.setCurrentId(89L);

        adminOperationLogService.recordProductInventoryAdjustment(20L, 10, 15, "補貨");

        ArgumentCaptor<AdminOperationLog> captor = ArgumentCaptor.forClass(AdminOperationLog.class);
        verify(adminOperationLogMapper).insert(captor.capture());
        AdminOperationLog log = captor.getValue();
        assertEquals("PRODUCT_INVENTORY_ADJUST", log.getAction());
        assertEquals("PRODUCT", log.getTargetType());
        assertEquals(20L, log.getTargetId());
        assertEquals("10", log.getBeforeValue());
        assertEquals("15", log.getAfterValue());
        assertEquals("補貨", log.getReason());
        assertEquals("ADMIN", log.getOperatorType());
        assertEquals(89L, log.getOperatorId());
        assertNotNull(log.getCreatedAt());
    }
}
