package com.localfresh.service.impl;

import com.github.pagehelper.Page;
import com.localfresh.dto.PaymentEventPageQueryDTO;
import com.localfresh.entity.PaymentEvent;
import com.localfresh.mapper.PaymentEventMapper;
import com.localfresh.result.PageResult;
import com.localfresh.vo.PaymentEventVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentEventServiceImplTest {

    @Mock
    private PaymentEventMapper paymentEventMapper;

    @InjectMocks
    private PaymentEventServiceImpl paymentEventService;

    @Test
    void pageQueryShouldReturnPaymentEventsAsPageResult() {
        PaymentEventPageQueryDTO queryDTO = new PaymentEventPageQueryDTO();
        queryDTO.setPage(1);
        queryDTO.setPageSize(10);
        queryDTO.setOrderNumber("ORDER-100");

        Page<PaymentEvent> page = new Page<>();
        page.setTotal(1);
        page.add(PaymentEvent.builder()
                .id(9L)
                .orderId(10L)
                .orderNumber("ORDER-100")
                .provider("DEMO")
                .eventType(PaymentEvent.EVENT_CALLBACK_SUCCEEDED)
                .providerTradeNo("DEMO-TRADE-100")
                .idempotencyKey("DEMO:CALLBACK_SUCCEEDED:ORDER-100:DEMO-TRADE-100")
                .amount(new BigDecimal("320.00"))
                .result(PaymentEvent.RESULT_SUCCEEDED)
                .build());
        when(paymentEventMapper.pageQuery(queryDTO)).thenReturn(page);

        PageResult result = paymentEventService.pageQuery(queryDTO);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        PaymentEventVO vo = (PaymentEventVO) result.getRecords().get(0);
        assertEquals(9L, vo.getId());
        assertEquals("ORDER-100", vo.getOrderNumber());
        assertEquals("DEMO", vo.getProvider());
        assertEquals(PaymentEvent.EVENT_CALLBACK_SUCCEEDED, vo.getEventType());
        assertEquals(PaymentEvent.RESULT_SUCCEEDED, vo.getResult());
        assertEquals("DEMO-TRADE-100", vo.getProviderTradeNo());
        assertEquals("DEMO:CALLBACK_SUCCEEDED:ORDER-100:DEMO-TRADE-100", vo.getIdempotencyKey());
    }
}
