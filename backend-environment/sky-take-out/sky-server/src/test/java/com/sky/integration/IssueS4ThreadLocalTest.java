package com.sky.integration;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.entity.Orders;
import com.sky.test.support.LoginResult;
import com.sky.mapper.OrderMapper;
import com.sky.websocket.WebSocketServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class IssueS4ThreadLocalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderMapper orderMapper;

    @MockitoBean(name = "redisTemplate")
    @SuppressWarnings("rawtypes")
    private RedisTemplate redisTemplate;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockitoBean
    private ServerEndpointExporter serverEndpointExporter;

    @MockitoBean
    private WebSocketServer webSocketServer;

    private String tokenA;
    private String tokenB;
    private Long userAId;
    private Orders orderA;

    @BeforeEach
    void setUp() throws Exception {
        LoginResult userA = login("s4-user-a");
        tokenA = userA.token();
        userAId = userA.userId();

        LoginResult userB = login("s4-user-b");
        tokenB = userB.token();

        orderA = insertOrder(userAId, Orders.COMPLETED, LocalDateTime.now());
    }

    @Test
    void noLeakBetweenRequests() throws Exception {
        mockMvc.perform(get("/user/order/orderDetail/{id}", orderA.getId())
                        .header("authentication", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/user/order/orderDetail/{id}", orderA.getId())
                        .header("authentication", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        mockMvc.perform(get("/user/order/orderDetail/{id}", orderA.getId())
                        .header("authentication", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
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

    private Orders insertOrder(Long userId, Integer status, LocalDateTime orderTime) {
        Orders order = new Orders();
        order.setNumber("TEST-" + System.nanoTime());
        order.setStatus(status);
        order.setUserId(userId);
        order.setOrderTime(orderTime);
        order.setPayMethod(1);
        order.setPayStatus(Orders.UN_PAID);
        order.setAmount(new BigDecimal("100.00"));
        order.setPhone("0912345678");
        order.setAddress("台北市測試地址");
        order.setConsignee("測試收件人");
        order.setDeliveryStatus(1);
        orderMapper.insert(order);
        return order;
    }

}
