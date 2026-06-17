package com.localfresh.integration;

import com.localfresh.constant.JwtClaimsConstant;
import com.localfresh.entity.OrderDetail;
import com.localfresh.entity.Orders;
import com.localfresh.mapper.OrderDetailMapper;
import com.localfresh.mapper.OrderMapper;
import com.localfresh.properties.JwtProperties;
import com.localfresh.utils.JwtUtil;
import com.localfresh.websocket.WebSocketServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class IssueS3SalesTop10Test {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private JwtProperties jwtProperties;

    @MockitoBean(name = "redisTemplate")
    @SuppressWarnings("rawtypes")
    private RedisTemplate redisTemplate;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockitoBean
    private ServerEndpointExporter serverEndpointExporter;

    @MockitoBean
    private WebSocketServer webSocketServer;

    private LocalDate beginDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {
        beginDate = LocalDate.now().minusDays(3);
        endDate = LocalDate.now().minusDays(1);

        Orders orderBegin = insertOrder(99L, Orders.COMPLETED, LocalDateTime.of(beginDate, LocalTime.NOON));
        insertOrderDetail(orderBegin.getId(), "S3_BeginDish", 2);

        Orders orderEnd = insertOrder(99L, Orders.COMPLETED, LocalDateTime.of(endDate, LocalTime.NOON));
        insertOrderDetail(orderEnd.getId(), "S3_EndDish", 3);
    }

    @Test
    void bothEdgeDateOrdersCountedInTop10() throws Exception {
        mockMvc.perform(get("/admin/report/top10")
                        .param("begin", beginDate.format(DATE_FORMATTER))
                        .param("end", endDate.format(DATE_FORMATTER))
                        .header("token", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.nameList", containsString("S3_BeginDish")))
                .andExpect(jsonPath("$.data.nameList", containsString("S3_EndDish")));
    }

    private String adminToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, 1L);
        return JwtUtil.createJWT(jwtProperties.getAdminSecretKey(), jwtProperties.getAdminTtl(), claims);
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
