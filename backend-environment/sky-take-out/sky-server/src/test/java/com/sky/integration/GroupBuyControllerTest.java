package com.sky.integration;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.exception.ForbiddenOperationException;
import com.sky.service.GroupBuyService;
import com.sky.test.support.LoginResult;
import com.sky.vo.GroupBuyParticipantVO;
import com.sky.vo.GroupBuyVO;
import com.sky.websocket.WebSocketServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GroupBuyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GroupBuyService groupBuyService;

    @MockitoBean(name = "redisTemplate")
    @SuppressWarnings("rawtypes")
    private RedisTemplate redisTemplate;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockitoBean
    private ServerEndpointExporter serverEndpointExporter;

    @MockitoBean
    private WebSocketServer webSocketServer;

    private String token;
    private GroupBuyVO groupBuyVO;

    @BeforeEach
    void setUp() throws Exception {
        token = login("groupbuy-controller-user").token();
        groupBuyVO = GroupBuyVO.builder()
                .id(1L)
                .groupNo("GB123456")
                .initiatorId(1L)
                .productId(7L)
                .productName("高山高麗菜")
                .productImage("https://example.com/cabbage.jpg")
                .quantity(2)
                .status(1)
                .currentCount(1)
                .requiredCount(3)
                .expireAt(LocalDateTime.of(2026, 5, 6, 12, 0))
                .shareUrl("http://localhost:5173/groupBuy/GB123456")
                .participants(List.of(GroupBuyParticipantVO.builder()
                        .memberId(1L)
                        .memberName("測試會員")
                        .joinedAt(LocalDateTime.of(2026, 5, 5, 12, 0))
                        .build()))
                .build();
    }

    @Test
    void initiate_returnsShareUrl() throws Exception {
        when(groupBuyService.initiate(any())).thenReturn(groupBuyVO);

        mockMvc.perform(post("/user/groupBuy/initiate")
                        .header("authentication", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"quantity\":2,\"addressId\":3,\"requiredCount\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.groupNo").value("GB123456"))
                .andExpect(jsonPath("$.data.shareUrl").value("http://localhost:5173/groupBuy/GB123456"));
    }

    @Test
    void initiate_shouldRejectInvalidQuantity() throws Exception {
        mockMvc.perform(post("/user/groupBuy/initiate")
                        .header("authentication", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"quantity\":0,\"addressId\":3,\"requiredCount\":3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void join_returnsGroupBuyVo_withShareUrl() throws Exception {
        groupBuyVO.setShareUrl("http://localhost:5173/groupBuy/GB123456");
        when(groupBuyService.joinGroupBuy(any())).thenReturn(groupBuyVO);

        mockMvc.perform(post("/user/groupBuy/join")
                        .header("authentication", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupNo\":\"GB123456\",\"productId\":1,\"quantity\":1,\"addressId\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.groupNo").value("GB123456"))
                .andExpect(jsonPath("$.data.shareUrl").value("http://localhost:5173/groupBuy/GB123456"));
    }

    @Test
    void cancel_returnsGroupBuyVo() throws Exception {
        when(groupBuyService.cancelGroupBuy("GB123456")).thenReturn(groupBuyVO);

        mockMvc.perform(post("/user/groupBuy/GB123456/cancel")
                        .header("authentication", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.groupNo").value("GB123456"));
    }

    @Test
    void cancel_whenNotInitiator_returns403() throws Exception {
        when(groupBuyService.cancelGroupBuy("GB123456"))
                .thenThrow(new ForbiddenOperationException("只有發起人可以取消揪團"));

        mockMvc.perform(post("/user/groupBuy/GB123456/cancel")
                        .header("authentication", token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("只有發起人可以取消揪團"));
    }

    @Test
    void getByGroupNo_returnsGroupBuyVo() throws Exception {
        groupBuyVO.setShareUrl("http://localhost:5173/groupBuy/GB123456");
        when(groupBuyService.getByGroupNo("GB123456")).thenReturn(groupBuyVO);

        mockMvc.perform(get("/user/groupBuy/GB123456")
                        .header("authentication", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.currentCount").value(1))
                .andExpect(jsonPath("$.data.productId").value(7))
                .andExpect(jsonPath("$.data.productName").value("高山高麗菜"))
                .andExpect(jsonPath("$.data.productImage").value("https://example.com/cabbage.jpg"))
                .andExpect(jsonPath("$.data.quantity").value(2))
                .andExpect(jsonPath("$.data.shareUrl").value("http://localhost:5173/groupBuy/GB123456"))
                .andExpect(jsonPath("$.data.participants[0].memberName").value("測試會員"));
    }

    @Test
    void listMyGroupBuys_returnsGroupBuyList() throws Exception {
        groupBuyVO.setShareUrl("http://localhost:5173/groupBuy/GB123456");
        when(groupBuyService.listMyGroupBuys()).thenReturn(List.of(groupBuyVO));

        mockMvc.perform(get("/user/groupBuy/my")
                        .header("authentication", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data[0].groupNo").value("GB123456"))
                .andExpect(jsonPath("$.data[0].shareUrl").value("http://localhost:5173/groupBuy/GB123456"));
    }

    @Test
    void initiate_withoutLogin_returns401() throws Exception {
        mockMvc.perform(post("/user/groupBuy/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"quantity\":2,\"addressId\":3,\"requiredCount\":3}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cancel_withoutLogin_returns401() throws Exception {
        mockMvc.perform(post("/user/groupBuy/GB123456/cancel"))
                .andExpect(status().isUnauthorized());
    }

    private LoginResult login(String code) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/user/member/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andReturn();

        JSONObject data = JSON.parseObject(loginResult.getResponse().getContentAsString())
                .getJSONObject("data");
        return new LoginResult(data.getLong("id"), data.getString("token"));
    }
}
