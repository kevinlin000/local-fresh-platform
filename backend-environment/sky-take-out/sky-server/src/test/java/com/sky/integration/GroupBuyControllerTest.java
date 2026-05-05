package com.sky.integration;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.service.GroupBuyService;
import com.sky.test.support.LoginResult;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.GroupBuyParticipantVO;
import com.sky.vo.GroupBuyVO;
import com.sky.websocket.WebSocketServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

    @MockBean
    private GroupBuyService groupBuyService;

    @MockBean(name = "redisTemplate")
    @SuppressWarnings("rawtypes")
    private RedisTemplate redisTemplate;

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockBean
    private ServerEndpointExporter serverEndpointExporter;

    @MockBean
    private WebSocketServer webSocketServer;

    @MockBean
    private WeChatPayUtil weChatPayUtil;

    private String token;
    private GroupBuyVO groupBuyVO;

    @BeforeEach
    void setUp() throws Exception {
        token = login("groupbuy-controller-user").token();
        groupBuyVO = GroupBuyVO.builder()
                .id(1L)
                .groupNo("GB123456")
                .initiatorId(1L)
                .status(1)
                .currentCount(1)
                .requiredCount(3)
                .expireAt(LocalDateTime.of(2026, 5, 6, 12, 0))
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
    void join_returnsGroupBuyVo_withoutShareUrl() throws Exception {
        when(groupBuyService.joinGroupBuy(any())).thenReturn(groupBuyVO);

        mockMvc.perform(post("/user/groupBuy/join")
                        .header("authentication", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupNo\":\"GB123456\",\"productId\":1,\"quantity\":1,\"addressId\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.groupNo").value("GB123456"))
                .andExpect(jsonPath("$.data.shareUrl").doesNotExist());
    }

    @Test
    void getByGroupNo_returnsGroupBuyVo() throws Exception {
        when(groupBuyService.getByGroupNo("GB123456")).thenReturn(groupBuyVO);

        mockMvc.perform(get("/user/groupBuy/GB123456")
                        .header("authentication", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.currentCount").value(1))
                .andExpect(jsonPath("$.data.participants[0].memberName").value("測試會員"));
    }

    @Test
    void listMyGroupBuys_returnsGroupBuyList() throws Exception {
        when(groupBuyService.listMyGroupBuys()).thenReturn(List.of(groupBuyVO));

        mockMvc.perform(get("/user/groupBuy/my")
                        .header("authentication", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data[0].groupNo").value("GB123456"));
    }

    @Test
    void initiate_withoutLogin_returns401() throws Exception {
        mockMvc.perform(post("/user/groupBuy/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"quantity\":2,\"addressId\":3,\"requiredCount\":3}"))
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
