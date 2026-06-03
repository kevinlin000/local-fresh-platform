package com.sky.integration;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.entity.Cart;
import com.sky.entity.Product;
import com.sky.entity.ProductSpec;
import com.sky.mapper.CartMapper;
import com.sky.mapper.ProductMapper;
import com.sky.mapper.ProductSpecMapper;
import com.sky.test.support.LoginResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductCartApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductSpecMapper productSpecMapper;

    @Autowired
    private CartMapper cartMapper;

    @MockBean
    private ServerEndpointExporter serverEndpointExporter;

    private LoginResult loginResult;
    private Product product;

    @BeforeEach
    void setUp() throws Exception {
        loginResult = login("product-cart-api-user");

        product = Product.builder()
                .productName("有機小黃瓜")
                .categoryId(1L)
                .price(new BigDecimal("88.00"))
                .description("脆口清甜")
                .status(1)
                .build();
        productMapper.insert(product);

        productSpecMapper.insertBatch(List.of(
                ProductSpec.builder()
                        .productId(product.getId())
                        .name("重量")
                        .value("500g")
                        .build()
        ));
    }

    @Test
    void productDetailShouldReturnProductSpecs() throws Exception {
        mockMvc.perform(get("/user/product/{id}", product.getId())
                        .header("authentication", loginResult.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(1)))
                .andExpect(jsonPath("$.data.id", is(product.getId().intValue())))
                .andExpect(jsonPath("$.data.productName", is("有機小黃瓜")))
                .andExpect(jsonPath("$.data.productSpecs", hasSize(1)))
                .andExpect(jsonPath("$.data.productSpecs[0].name", is("重量")))
                .andExpect(jsonPath("$.data.productSpecs[0].value", is("500g")));
    }

    @Test
    void cartSubShouldDecreaseQuantityWhenMoreThanOne() throws Exception {
        addToCart();
        addToCart();

        mockMvc.perform(post("/user/cart/sub")
                        .header("authentication", loginResult.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CartRequest(product.getId(), null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(1)));

        List<Cart> carts = cartMapper.list(Cart.builder()
                .userId(loginResult.userId())
                .productId(product.getId())
                .build());
        assertEquals(1, carts.size());
        assertEquals(1, carts.get(0).getNumber());
    }

    @Test
    void cartSubShouldDeleteItemWhenQuantityReachesZero() throws Exception {
        addToCart();

        mockMvc.perform(post("/user/cart/sub")
                        .header("authentication", loginResult.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CartRequest(product.getId(), null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(1)));

        List<Cart> carts = cartMapper.list(Cart.builder()
                .userId(loginResult.userId())
                .productId(product.getId())
                .build());
        assertTrue(carts.isEmpty());
    }

    @Test
    void cartAddShouldRejectMissingProductAndGiftBox() throws Exception {
        mockMvc.perform(post("/user/cart/add")
                        .header("authentication", loginResult.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(0)));
    }

    private void addToCart() throws Exception {
        mockMvc.perform(post("/user/cart/add")
                        .header("authentication", loginResult.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CartRequest(product.getId(), null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(1)));
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

    private static class CartRequest {
        private final Long productId;
        private final Long giftBoxId;
        private final String productSpec;

        private CartRequest(Long productId, Long giftBoxId, String productSpec) {
            this.productId = productId;
            this.giftBoxId = giftBoxId;
            this.productSpec = productSpec;
        }

        public Long getProductId() {
            return productId;
        }

        public Long getGiftBoxId() {
            return giftBoxId;
        }

        public String getProductSpec() {
            return productSpec;
        }
    }
}
