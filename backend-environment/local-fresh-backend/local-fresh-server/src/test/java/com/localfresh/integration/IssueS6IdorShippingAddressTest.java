package com.localfresh.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.localfresh.entity.ShippingAddress;
import com.localfresh.integration.support.MockRedissonInfrastructureIntegrationTest;
import com.localfresh.mapper.ShippingAddressMapper;
import com.localfresh.test.support.LoginResult;
import com.localfresh.utils.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class IssueS6IdorShippingAddressTest extends MockRedissonInfrastructureIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShippingAddressMapper shippingAddressMapper;

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

        JsonNode data = JsonUtil.readTree(loginResult.getResponse().getContentAsString()).path("data");
        return new LoginResult(data.path("id").asLong(), data.path("token").asText());
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
