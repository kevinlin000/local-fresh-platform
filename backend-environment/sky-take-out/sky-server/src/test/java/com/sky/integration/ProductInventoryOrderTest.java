package com.sky.integration;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.entity.Cart;
import com.sky.entity.Product;
import com.sky.entity.ShippingAddress;
import com.sky.mapper.CartMapper;
import com.sky.mapper.ProductMapper;
import com.sky.mapper.ShippingAddressMapper;
import com.sky.test.support.LoginResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductInventoryOrderTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private ShippingAddressMapper shippingAddressMapper;

    @MockitoBean
    private ServerEndpointExporter serverEndpointExporter;

    private LoginResult loginResult;
    private Product product;
    private ShippingAddress address;

    @BeforeEach
    void setUp() throws Exception {
        loginResult = login("inventory-order-user");

        product = Product.builder()
                .productName("庫存測試高麗菜")
                .categoryId(1L)
                .price(new BigDecimal("60.00"))
                .description("庫存扣減測試")
                .status(1)
                .stock(3)
                .lowStockThreshold(1)
                .build();
        productMapper.insert(product);

        address = ShippingAddress.builder()
                .memberId(loginResult.userId())
                .consignee("庫存測試會員")
                .phone("0912345678")
                .cityName("台北市")
                .districtName("信義區")
                .detail("市府路 1 號")
                .label("測試地址")
                .isDefault(1)
                .build();
        shippingAddressMapper.insert(address);
    }

    @Test
    void submitOrderShouldReserveProductStockAndUserCancelShouldRestoreIt() throws Exception {
        addCart(2);

        MvcResult submitResult = mockMvc.perform(post("/user/order/submit")
                        .header("authentication", loginResult.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderSubmitRequest(new BigDecimal("120.00")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andReturn();

        assertEquals(1, productMapper.getById(product.getId()).getStock());

        Long orderId = JSON.parseObject(submitResult.getResponse().getContentAsString())
                .getJSONObject("data")
                .getLong("id");
        mockMvc.perform(put("/user/order/cancel/{id}", orderId)
                        .header("authentication", loginResult.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        assertEquals(3, productMapper.getById(product.getId()).getStock());
    }

    @Test
    void submitOrderShouldRejectWhenProductStockIsNotEnough() throws Exception {
        addCart(4);

        mockMvc.perform(post("/user/order/submit")
                        .header("authentication", loginResult.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderSubmitRequest(new BigDecimal("240.00")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("商品庫存不足"));

        assertEquals(3, productMapper.getById(product.getId()).getStock());
    }

    private void addCart(int quantity) {
        cartMapper.insert(Cart.builder()
                .name(product.getProductName())
                .userId(loginResult.userId())
                .productId(product.getId())
                .number(quantity)
                .amount(product.getPrice())
                .createTime(LocalDateTime.now())
                .build());
    }

    private Map<String, Object> orderSubmitRequest(BigDecimal amount) {
        return Map.of(
                "addressBookId", address.getId(),
                "payMethod", 1,
                "remark", "inventory test",
                "deliveryStatus", 1,
                "tablewareNumber", 0,
                "tablewareStatus", 1,
                "packAmount", 0,
                "amount", amount
        );
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
