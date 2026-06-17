package com.localfresh.integration;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.localfresh.entity.OrderDetail;
import com.localfresh.test.support.LoginResult;
import com.localfresh.entity.Orders;
import com.localfresh.mapper.OrderDetailMapper;
import com.localfresh.mapper.OrderMapper;
import com.localfresh.websocket.WebSocketServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class IssueS2IdorOrderTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @MockitoBean(name = "redisTemplate")
    @SuppressWarnings("rawtypes")
    private RedisTemplate redisTemplate;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockitoBean
    private ServerEndpointExporter serverEndpointExporter;

    @MockitoBean
    private WebSocketServer webSocketServer;

    @MockitoBean
    private RedissonClient redissonClient;

    private String tokenA;
    private String tokenB;
    private Long userAId;
    private Long userBId;
    private Orders orderA;

    @BeforeEach
    void setUp() throws Exception {
        LoginResult userA = login("s2-user-a");
        tokenA = userA.token();
        userAId = userA.userId();

        LoginResult userB = login("s2-user-b");
        tokenB = userB.token();
        userBId = userB.userId();

        orderA = insertOrder(userAId, Orders.COMPLETED, LocalDateTime.now());
        insertOrderDetail(orderA.getId(), "測試菜品A", 1);
    }

    @Test
    void userA_canRead_ownOrder() throws Exception {
        mockMvc.perform(get("/user/order/orderDetail/{id}", orderA.getId())
                        .header("authentication", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    void userB_cannotRead_userA_order() throws Exception {
        mockMvc.perform(get("/user/order/orderDetail/{id}", orderA.getId())
                        .header("authentication", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void userB_cannotRepetition_userA_order() throws Exception {
        mockMvc.perform(post("/user/order/repetition/{id}", orderA.getId())
                        .header("authentication", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void userB_cannotReminder_userA_order() throws Exception {
        mockMvc.perform(get("/user/order/reminder/{id}", orderA.getId())
                        .header("authentication", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void userB_cannotCancel_userA_order() throws Exception {
        Orders pendingOrder = insertOrder(userAId, Orders.PENDING_PAYMENT, LocalDateTime.now());

        mockMvc.perform(put("/user/order/cancel/{id}", pendingOrder.getId())
                        .header("authentication", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        Orders orderAfterCancelAttempt = orderMapper.getById(pendingOrder.getId());
        org.junit.jupiter.api.Assertions.assertEquals(Orders.PENDING_PAYMENT, orderAfterCancelAttempt.getStatus());
    }

    @Test
    void userB_cannotPay_userA_order() throws Exception {
        Orders pendingOrder = insertOrder(userAId, Orders.PENDING_PAYMENT, LocalDateTime.now());

        mockMvc.perform(put("/user/order/payment")
                        .header("authentication", tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderNumber\":\"" + pendingOrder.getNumber() + "\",\"payMethod\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        Orders orderAfterPaymentAttempt = orderMapper.getById(pendingOrder.getId());
        org.junit.jupiter.api.Assertions.assertEquals(Orders.PENDING_PAYMENT, orderAfterPaymentAttempt.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(Orders.UN_PAID, orderAfterPaymentAttempt.getPayStatus());
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

    private void insertOrderDetail(Long orderId, String name, int number) {
        OrderDetail detail = new OrderDetail();
        detail.setOrderId(orderId);
        detail.setName(name);
        detail.setNumber(number);
        detail.setAmount(new BigDecimal("50.00"));
        orderDetailMapper.insert(detail);
    }

}
