package com.sky.integration;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.entity.ShippingAddress;
import com.sky.mapper.ShippingAddressMapper;
import com.sky.test.support.LoginResult;
import com.sky.websocket.WebSocketServer;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class IssueS6IdorShippingAddressTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShippingAddressMapper shippingAddressMapper;

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

    private String tokenB;
    private Long userAId;
    private ShippingAddress addressA;

    @BeforeEach
    void setUp() throws Exception {
        LoginResult userA = login("s6-user-a");
        userAId = userA.userId();

        LoginResult userB = login("s6-user-b");
        tokenB = userB.token();

        addressA = insertAddress(userAId, "會員A地址", 0);
    }

    @Test
    void userB_cannotRead_userA_address() throws Exception {
        mockMvc.perform(get("/user/shippingAddress/{id}", addressA.getId())
                        .header("authentication", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void userB_cannotUpdate_userA_address() throws Exception {
        mockMvc.perform(put("/user/shippingAddress")
                        .header("authentication", tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + addressA.getId() + ",\"consignee\":\"惡意修改\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        ShippingAddress addressAfterUpdateAttempt = shippingAddressMapper.getById(addressA.getId());
        assertEquals("會員A地址", addressAfterUpdateAttempt.getConsignee());
    }

    @Test
    void userB_cannotDelete_userA_address() throws Exception {
        mockMvc.perform(delete("/user/shippingAddress")
                        .header("authentication", tokenB)
                        .param("id", String.valueOf(addressA.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        assertNotNull(shippingAddressMapper.getById(addressA.getId()));
    }

    @Test
    void userB_cannotSetDefault_userA_address() throws Exception {
        mockMvc.perform(put("/user/shippingAddress/default")
                        .header("authentication", tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + addressA.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        ShippingAddress addressAfterDefaultAttempt = shippingAddressMapper.getById(addressA.getId());
        assertEquals(0, addressAfterDefaultAttempt.getIsDefault());
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

    private ShippingAddress insertAddress(Long memberId, String consignee, Integer isDefault) {
        ShippingAddress address = new ShippingAddress();
        address.setMemberId(memberId);
        address.setConsignee(consignee);
        address.setPhone("0912345678");
        address.setCityName("台北市");
        address.setDistrictName("信義區");
        address.setDetail("市府路1號");
        address.setIsDefault(isDefault);
        shippingAddressMapper.insert(address);
        return address;
    }
}
