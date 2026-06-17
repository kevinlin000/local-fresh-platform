package com.localfresh.integration;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.localfresh.entity.Cart;
import com.localfresh.entity.Product;
import com.localfresh.entity.ProductSpec;
import com.localfresh.mapper.CartMapper;
import com.localfresh.mapper.ProductMapper;
import com.localfresh.mapper.ProductSpecMapper;
import com.localfresh.test.support.LoginResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

    @MockitoBean
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
    void productListShouldSearchEnabledProductsAcrossCategories() throws Exception {
        Product matchingProduct = Product.builder()
                .productName("高山有機菠菜")
                .categoryId(2L)
                .price(new BigDecimal("120.00"))
                .description("清甜葉菜")
                .status(1)
                .build();
        productMapper.insert(matchingProduct);

        Product disabledProduct = Product.builder()
                .productName("有機停售番茄")
                .categoryId(2L)
                .price(new BigDecimal("150.00"))
                .description("停售商品")
                .status(0)
                .build();
        productMapper.insert(disabledProduct);

        mockMvc.perform(get("/user/product/list")
                        .header("authentication", loginResult.token())
                        .param("productName", "有機"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(1)))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[?(@.productName == '有機小黃瓜')]", hasSize(1)))
                .andExpect(jsonPath("$.data[?(@.productName == '高山有機菠菜')]", hasSize(1)))
                .andExpect(jsonPath("$.data[?(@.productName == '有機停售番茄')]", hasSize(0)));
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
