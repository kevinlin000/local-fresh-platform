package com.localfresh.integration;

import com.localfresh.constant.JwtClaimsConstant;
import com.localfresh.entity.Product;
import com.localfresh.integration.support.MockWebSocketMvcIntegrationTest;
import com.localfresh.mapper.ProductMapper;
import com.localfresh.properties.JwtProperties;
import com.localfresh.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class WorkspaceLowStockTest extends MockWebSocketMvcIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtProperties jwtProperties;

    private Long categoryId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("""
                insert into category (type, name, sort, status, create_time, update_time)
                values (1, '低庫存測試分類', 1, 1, now(), now())
                """);
        categoryId = jdbcTemplate.queryForObject(
                "select id from category where name = '低庫存測試分類' order by id desc limit 1",
                Long.class);

        insertProduct("嚴重缺貨菠菜", 1, 5);
        insertProduct("低庫存玉米", 4, 5);
        insertProduct("庫存正常番茄", 20, 5);
    }

    @Test
    void lowStockProductsShouldReturnOnlyProductsAtOrBelowThreshold() throws Exception {
        mockMvc.perform(get("/admin/workspace/lowStockProducts")
                        .header("token", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].productName").value("嚴重缺貨菠菜"))
                .andExpect(jsonPath("$.data[0].stock").value(1))
                .andExpect(jsonPath("$.data[0].lowStockThreshold").value(5))
                .andExpect(jsonPath("$.data[0].categoryName").value("低庫存測試分類"))
                .andExpect(jsonPath("$.data[1].productName").value("低庫存玉米"));

        mockMvc.perform(get("/admin/product/page")
                        .param("page", "1")
                        .param("pageSize", "10")
                        .param("lowStock", "true")
                        .header("token", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[0].productName").value("嚴重缺貨菠菜"))
                .andExpect(jsonPath("$.data.records[1].productName").value("低庫存玉米"));
    }

    private void insertProduct(String name, int stock, int lowStockThreshold) {
        Product product = Product.builder()
                .productName(name)
                .categoryId(categoryId)
                .price(new BigDecimal("50.00"))
                .description("低庫存測試")
                .status(1)
                .stock(stock)
                .lowStockThreshold(lowStockThreshold)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        productMapper.insert(product);
    }

    private String adminToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, 1L);
        return JwtUtil.createJWT(jwtProperties.getAdminSecretKey(), jwtProperties.getAdminTtl(), claims);
    }
}
