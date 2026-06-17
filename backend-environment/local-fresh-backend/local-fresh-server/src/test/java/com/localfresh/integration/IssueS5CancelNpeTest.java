package com.localfresh.integration;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.localfresh.websocket.WebSocketServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate; //NOSONAR raw type matches bean definition
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * S-5: 取消不存在的訂單，應拋出 OrderBusinessException 回傳 code=0，
 * 而非未捕捉的 NullPointerException 導致 HTTP 500。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Rollback
class IssueS5CancelNpeTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Redis 是基礎設施層，業務邏輯正確性不依賴 Redis 狀態，
     * 用 Mock 隔離以確保測試環境可重複且不需要真實 Redis 連線。
     * 使用 raw type 以匹配 RedisConfiguration 定義的 bean 型別。
     */
    @MockitoBean(name = "redisTemplate")
    @SuppressWarnings("rawtypes")
    private RedisTemplate redisTemplate;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    /**
     * ServerEndpointExporter 在 MOCK Web 環境（無真實 Servlet 容器）會失敗，
     * 需要 Mock 掉以讓 WebSocket 端點不被真正注冊。
     */
    @MockitoBean
    private ServerEndpointExporter serverEndpointExporter;

    @MockitoBean
    private WebSocketServer webSocketServer;

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
        JSONObject json = JSON.parseObject(body);
        userToken = json.getJSONObject("data").getString("token");
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
