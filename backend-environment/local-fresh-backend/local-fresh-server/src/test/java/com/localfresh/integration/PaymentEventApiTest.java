package com.localfresh.integration;

import com.localfresh.constant.JwtClaimsConstant;
import com.localfresh.entity.PaymentEvent;
import com.localfresh.properties.JwtProperties;
import com.localfresh.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaymentEventApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtProperties jwtProperties;

    @MockitoBean
    private ServerEndpointExporter serverEndpointExporter;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("""
                insert into payment_event
                    (order_id, order_number, provider, event_type, provider_reference, amount, result, raw_payload, created_at)
                values
                    (100, 'ORDER-API-1', 'DEMO', ?, 'demo-paid:ORDER-API-1', 300.00, ?, '{}', '2026-01-02 10:00:00'),
                    (101, 'ORDER-API-1', 'DEMO', ?, null, 300.00, ?, null, '2026-01-02 10:01:00'),
                    (102, 'ORDER-API-2', 'ECPAY', ?, 'ecpay:ORDER-API-2', 500.00, ?, '{}', '2026-01-03 10:00:00')
                """,
                PaymentEvent.EVENT_REQUEST_CREATED,
                PaymentEvent.RESULT_PENDING,
                PaymentEvent.EVENT_CALLBACK_SUCCEEDED,
                PaymentEvent.RESULT_SUCCEEDED,
                PaymentEvent.EVENT_REQUEST_CREATED,
                PaymentEvent.RESULT_PENDING);
    }

    @Test
    void pageShouldFilterPaymentEventsAndReturnNewestFirst() throws Exception {
        mockMvc.perform(get("/admin/paymentEvents/page")
                        .param("page", "1")
                        .param("pageSize", "10")
                        .param("orderNumber", "ORDER-API-1")
                        .param("provider", "DEMO")
                        .param("beginTime", "2026-01-01 00:00:00")
                        .param("endTime", "2026-01-05 00:00:00")
                        .header("token", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[0].orderNumber").value("ORDER-API-1"))
                .andExpect(jsonPath("$.data.records[0].provider").value("DEMO"))
                .andExpect(jsonPath("$.data.records[0].eventType").value(PaymentEvent.EVENT_CALLBACK_SUCCEEDED))
                .andExpect(jsonPath("$.data.records[0].result").value(PaymentEvent.RESULT_SUCCEEDED))
                .andExpect(jsonPath("$.data.records[1].eventType").value(PaymentEvent.EVENT_REQUEST_CREATED))
                .andExpect(jsonPath("$.data.records[1].providerReference").value("demo-paid:ORDER-API-1"));
    }

    private String adminToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, 7L);
        return JwtUtil.createJWT(jwtProperties.getAdminSecretKey(), jwtProperties.getAdminTtl(), claims);
    }
}
