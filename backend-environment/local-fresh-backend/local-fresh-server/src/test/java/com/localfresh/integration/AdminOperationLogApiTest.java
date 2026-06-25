package com.localfresh.integration;

import com.localfresh.constant.JwtClaimsConstant;
import com.localfresh.integration.support.MockWebSocketMvcIntegrationTest;
import com.localfresh.properties.JwtProperties;
import com.localfresh.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class AdminOperationLogApiTest extends MockWebSocketMvcIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("""
                insert into admin_operation_log
                    (action, target_type, target_id, before_value, after_value, reason, operator_type, operator_id, created_at)
                values
                    ('INVENTORY_ADJUST', 'PRODUCT', 100, '3', '8', '進貨補貨', 'ADMIN', 7, '2026-01-02 10:00:00'),
                    ('ORDER_CONFIRM', 'ORDER', 200, '2', '3', null, 'ADMIN', 8, '2026-01-03 10:00:00'),
                    ('INVENTORY_ADJUST', 'PRODUCT', 101, '8', '6', '盤點耗損', 'ADMIN', 7, '2026-01-04 10:00:00')
                """);
    }

    @Test
    void pageShouldFilterAuditLogsAndReturnNewestFirst() throws Exception {
        mockMvc.perform(get("/admin/operationLogs/page")
                        .param("page", "1")
                        .param("pageSize", "10")
                        .param("action", "INVENTORY_ADJUST")
                        .param("targetType", "PRODUCT")
                        .param("operatorId", "7")
                        .param("beginTime", "2026-01-01 00:00:00")
                        .param("endTime", "2026-01-05 00:00:00")
                        .header("token", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[0].action").value("INVENTORY_ADJUST"))
                .andExpect(jsonPath("$.data.records[0].targetType").value("PRODUCT"))
                .andExpect(jsonPath("$.data.records[0].targetId").value(101))
                .andExpect(jsonPath("$.data.records[0].beforeValue").value("8"))
                .andExpect(jsonPath("$.data.records[0].afterValue").value("6"))
                .andExpect(jsonPath("$.data.records[0].reason").value("盤點耗損"))
                .andExpect(jsonPath("$.data.records[0].operatorType").value("ADMIN"))
                .andExpect(jsonPath("$.data.records[0].operatorId").value(7))
                .andExpect(jsonPath("$.data.records[1].targetId").value(100))
                .andExpect(jsonPath("$.data.records[1].reason").value("進貨補貨"));
    }

    private String adminToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, 7L);
        return JwtUtil.createJWT(jwtProperties.getAdminSecretKey(), jwtProperties.getAdminTtl(), claims);
    }
}
