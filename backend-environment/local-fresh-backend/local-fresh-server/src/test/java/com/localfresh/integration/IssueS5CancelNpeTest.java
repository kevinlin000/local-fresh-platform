package com.localfresh.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.localfresh.integration.support.MockInfrastructureIntegrationTest;
import com.localfresh.utils.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * S-5: 取消不存在的訂單，應拋出 OrderBusinessException 回傳 code=0，
 * 而非未捕捉的 NullPointerException 導致 HTTP 500。
 */
@Transactional
@Rollback
class IssueS5CancelNpeTest extends MockInfrastructureIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        // 使用假登入取得 JWT（code 任意，openid 會寫成 mock_<code>）
        MvcResult loginResult = mockMvc.perform(
                post("/user/member/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"s5-test-user\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String body = loginResult.getResponse().getContentAsString();
        JsonNode json = JsonUtil.readTree(body);
        userToken = json.path("data").path("token").asText();
    }

    @Test
    void cancel_nonExistentOrder_returnsErrorCodeNotNpe() throws Exception {
        // 訂單 ID 99999 在 H2 中不存在
        // 預期：HTTP 200 + code=0（OrderBusinessException 被 GlobalExceptionHandler 捕捉）
        // 若 NPE 未捕捉，會回傳 HTTP 500
        mockMvc.perform(
                put("/user/order/cancel/99999")
                        .header("authentication", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("訂單不存在"));
    }
}
