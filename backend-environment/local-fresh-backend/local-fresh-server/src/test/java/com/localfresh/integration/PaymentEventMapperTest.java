package com.localfresh.integration;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.localfresh.dto.PaymentEventPageQueryDTO;
import com.localfresh.entity.PaymentEvent;
import com.localfresh.mapper.PaymentEventMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentEventMapperTest {

    @Autowired
    private PaymentEventMapper paymentEventMapper;

    @MockitoBean
    private ServerEndpointExporter serverEndpointExporter;

    @Test
    void insertAndListByOrderNumberShouldReturnNewestPaymentEventsFirst() {
        paymentEventMapper.insert(PaymentEvent.builder()
                .orderId(10L)
                .orderNumber("ORDER-100")
                .provider("DEMO")
                .eventType(PaymentEvent.EVENT_REQUEST_CREATED)
                .providerReference("demo-paid:ORDER-100")
                .idempotencyKey("DEMO:REQUEST_CREATED:ORDER-100:demo-paid:ORDER-100")
                .amount(new BigDecimal("320.00"))
                .result(PaymentEvent.RESULT_PENDING)
                .rawPayload("{\"packageStr\":\"demo-paid:ORDER-100\"}")
                .createdAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build());
        paymentEventMapper.insert(PaymentEvent.builder()
                .orderId(10L)
                .orderNumber("ORDER-100")
                .provider("DEMO")
                .eventType(PaymentEvent.EVENT_CALLBACK_SUCCEEDED)
                .providerTradeNo("DEMO-TRADE-100")
                .idempotencyKey("DEMO:CALLBACK_SUCCEEDED:ORDER-100:DEMO-TRADE-100")
                .amount(new BigDecimal("320.00"))
                .result(PaymentEvent.RESULT_SUCCEEDED)
                .createdAt(LocalDateTime.of(2026, 1, 1, 10, 1))
                .build());

        List<PaymentEvent> events = paymentEventMapper.listByOrderNumber("ORDER-100");

        assertEquals(2, events.size());
        assertEquals(PaymentEvent.EVENT_CALLBACK_SUCCEEDED, events.get(0).getEventType());
        assertEquals(PaymentEvent.RESULT_SUCCEEDED, events.get(0).getResult());
        assertEquals("DEMO-TRADE-100", events.get(0).getProviderTradeNo());
        assertEquals("DEMO:CALLBACK_SUCCEEDED:ORDER-100:DEMO-TRADE-100", events.get(0).getIdempotencyKey());
        assertEquals(PaymentEvent.EVENT_REQUEST_CREATED, events.get(1).getEventType());
        assertEquals("demo-paid:ORDER-100", events.get(1).getProviderReference());
        assertEquals("DEMO:REQUEST_CREATED:ORDER-100:demo-paid:ORDER-100", events.get(1).getIdempotencyKey());
        assertNotNull(events.get(0).getId());
        assertNotNull(events.get(1).getId());

        PaymentEventPageQueryDTO queryDTO = new PaymentEventPageQueryDTO();
        queryDTO.setPage(1);
        queryDTO.setPageSize(10);
        queryDTO.setProviderTradeNo("DEMO-TRADE-100");
        queryDTO.setIdempotencyKey("DEMO:CALLBACK_SUCCEEDED:ORDER-100:DEMO-TRADE-100");
        PageHelper.startPage(queryDTO.getPage(), queryDTO.getPageSize());
        Page<PaymentEvent> page = paymentEventMapper.pageQuery(queryDTO);

        assertEquals(1, page.size());
        assertEquals(PaymentEvent.EVENT_CALLBACK_SUCCEEDED, page.get(0).getEventType());
    }
}
