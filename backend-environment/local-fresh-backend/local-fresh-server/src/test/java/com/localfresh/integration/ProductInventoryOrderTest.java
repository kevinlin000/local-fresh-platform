package com.localfresh.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.localfresh.constant.JwtClaimsConstant;
import com.localfresh.entity.Cart;
import com.localfresh.entity.GiftBox;
import com.localfresh.entity.GiftBoxProduct;
import com.localfresh.entity.Product;
import com.localfresh.entity.ProductInventoryLog;
import com.localfresh.entity.ShippingAddress;
import com.localfresh.entity.Orders;
import com.localfresh.mapper.CartMapper;
import com.localfresh.mapper.GiftBoxMapper;
import com.localfresh.mapper.GiftBoxProductMapper;
import com.localfresh.mapper.OrderMapper;
import com.localfresh.mapper.ProductMapper;
import com.localfresh.mapper.ProductInventoryLogMapper;
import com.localfresh.mapper.ShippingAddressMapper;
import com.localfresh.properties.JwtProperties;
import com.localfresh.service.CacheService;
import com.localfresh.service.payment.PaymentGateway;
import com.localfresh.test.support.LoginResult;
import com.localfresh.utils.JwtUtil;
import com.localfresh.utils.JsonUtil;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    private ProductInventoryLogMapper productInventoryLogMapper;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private GiftBoxMapper giftBoxMapper;

    @Autowired
    private GiftBoxProductMapper giftBoxProductMapper;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private ShippingAddressMapper shippingAddressMapper;

    @Autowired
    private OrderMapper orderMapper;

    @MockitoBean
    private ServerEndpointExporter serverEndpointExporter;

    @MockitoBean
    private CacheService cacheService;

    @MockitoBean
    private PaymentGateway paymentGateway;

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

        Long orderId = JsonUtil.readTree(submitResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();
        assertEquals(1, productMapper.getById(product.getId()).getStock());
        List<ProductInventoryLog> logsAfterSubmit = productInventoryLogMapper.listByProductId(product.getId());
        assertEquals(1, logsAfterSubmit.size());
        assertInventoryLog(logsAfterSubmit.get(0), -2, 3, 1, "ORDER_RESERVE", orderId, "MEMBER");

        mockMvc.perform(put("/user/order/cancel/{id}", orderId)
                        .header("authentication", loginResult.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        assertEquals(3, productMapper.getById(product.getId()).getStock());
        List<ProductInventoryLog> logsAfterCancel = productInventoryLogMapper.listByProductId(product.getId());
        assertEquals(2, logsAfterCancel.size());
        assertInventoryLog(logsAfterCancel.get(1), 2, 1, 3, "ORDER_CANCEL_RESTORE", orderId, "MEMBER");

        mockMvc.perform(get("/admin/product/{id}/inventory-logs", product.getId())
                        .header("token", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].changeQuantity").value(-2))
                .andExpect(jsonPath("$.data[0].stockBefore").value(3))
                .andExpect(jsonPath("$.data[0].stockAfter").value(1))
                .andExpect(jsonPath("$.data[0].reason").value("ORDER_RESERVE"))
                .andExpect(jsonPath("$.data[0].referenceType").value("ORDER"))
                .andExpect(jsonPath("$.data[0].referenceId").value(orderId))
                .andExpect(jsonPath("$.data[0].operatorType").value("MEMBER"))
                .andExpect(jsonPath("$.data[0].operatorId").value(loginResult.userId()))
                .andExpect(jsonPath("$.data[1].changeQuantity").value(2))
                .andExpect(jsonPath("$.data[1].stockBefore").value(1))
                .andExpect(jsonPath("$.data[1].stockAfter").value(3))
                .andExpect(jsonPath("$.data[1].reason").value("ORDER_CANCEL_RESTORE"));
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
        assertEquals(0, productInventoryLogMapper.listByProductId(product.getId()).size());
    }

    @Test
    void adminShouldAdjustProductInventoryAndWriteAuditLog() throws Exception {
        mockMvc.perform(patch("/admin/product/{id}/inventory", product.getId())
                        .header("token", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"changeQuantity\":5,\"reason\":\"進貨補貨\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        assertEquals(8, productMapper.getById(product.getId()).getStock());
        List<ProductInventoryLog> logsAfterIncrease = productInventoryLogMapper.listByProductId(product.getId());
        assertEquals(1, logsAfterIncrease.size());
        assertManualInventoryLog(logsAfterIncrease.get(0), 5, 3, 8, "進貨補貨");

        mockMvc.perform(patch("/admin/product/{id}/inventory", product.getId())
                        .header("token", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"changeQuantity\":-2,\"reason\":\"盤點耗損\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        assertEquals(6, productMapper.getById(product.getId()).getStock());
        List<ProductInventoryLog> logsAfterDecrease = productInventoryLogMapper.listByProductId(product.getId());
        assertEquals(2, logsAfterDecrease.size());
        assertManualInventoryLog(logsAfterDecrease.get(1), -2, 8, 6, "盤點耗損");

        mockMvc.perform(patch("/admin/product/{id}/inventory", product.getId())
                        .header("token", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"changeQuantity\":-99,\"reason\":\"錯誤扣減\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("商品庫存不足"));

        assertEquals(6, productMapper.getById(product.getId()).getStock());
        assertEquals(2, productInventoryLogMapper.listByProductId(product.getId()).size());
    }

    @Test
    void submitGiftBoxOrderShouldReserveComponentProductStockAndCancelShouldRestoreIt() throws Exception {
        GiftBox giftBox = GiftBox.builder()
                .boxName("庫存測試直送箱")
                .categoryId(2L)
                .price(new BigDecimal("180.00"))
                .status(1)
                .description("內含兩份高麗菜")
                .build();
        giftBoxMapper.insert(giftBox);
        giftBoxProductMapper.insertBatch(List.of(GiftBoxProduct.builder()
                .giftBoxId(giftBox.getId())
                .productId(product.getId())
                .name(product.getProductName())
                .price(product.getPrice())
                .copies(2)
                .build()));

        cartMapper.insert(Cart.builder()
                .name(giftBox.getBoxName())
                .userId(loginResult.userId())
                .giftBoxId(giftBox.getId())
                .number(1)
                .amount(giftBox.getPrice())
                .createTime(LocalDateTime.now())
                .build());

        MvcResult submitResult = mockMvc.perform(post("/user/order/submit")
                        .header("authentication", loginResult.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderSubmitRequest(new BigDecimal("180.00")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andReturn();

        Long orderId = JsonUtil.readTree(submitResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();
        assertEquals(1, productMapper.getById(product.getId()).getStock());
        List<ProductInventoryLog> logsAfterSubmit = productInventoryLogMapper.listByProductId(product.getId());
        assertEquals(1, logsAfterSubmit.size());
        assertInventoryLog(logsAfterSubmit.get(0), -2, 3, 1, "ORDER_RESERVE", orderId, "MEMBER");

        mockMvc.perform(put("/user/order/cancel/{id}", orderId)
                        .header("authentication", loginResult.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        assertEquals(3, productMapper.getById(product.getId()).getStock());
        List<ProductInventoryLog> logsAfterCancel = productInventoryLogMapper.listByProductId(product.getId());
        assertEquals(2, logsAfterCancel.size());
        assertInventoryLog(logsAfterCancel.get(1), 2, 1, 3, "ORDER_CANCEL_RESTORE", orderId, "MEMBER");
    }

    @Test
    void adminRejectionShouldRefundRestoreStockAndWriteAdminAuditLog() throws Exception {
        addCart(2);

        MvcResult submitResult = mockMvc.perform(post("/user/order/submit")
                        .header("authentication", loginResult.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderSubmitRequest(new BigDecimal("120.00")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andReturn();

        Long orderId = JsonUtil.readTree(submitResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();
        Orders paidOrder = new Orders();
        paidOrder.setId(orderId);
        paidOrder.setStatus(Orders.TO_BE_CONFIRMED);
        paidOrder.setPayStatus(Orders.PAID);
        orderMapper.update(paidOrder);

        mockMvc.perform(put("/admin/order/rejection")
                        .header("token", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + orderId + ",\"rejectionReason\":\"商品售完\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        Orders canceled = orderMapper.getById(orderId);
        assertEquals(Orders.CANCELLED, canceled.getStatus());
        assertEquals(Orders.REFUND, canceled.getPayStatus());
        assertEquals("商品售完", canceled.getRejectionReason());
        assertNotNull(canceled.getCancelTime());
        verify(paymentGateway).refund(any(Orders.class), eq("商品售完"));
        assertEquals(3, productMapper.getById(product.getId()).getStock());

        List<ProductInventoryLog> logs = productInventoryLogMapper.listByProductId(product.getId());
        assertEquals(2, logs.size());
        ProductInventoryLog restoreLog = logs.get(1);
        assertEquals(2, restoreLog.getChangeQuantity());
        assertEquals(1, restoreLog.getStockBefore());
        assertEquals(3, restoreLog.getStockAfter());
        assertEquals("ORDER_CANCEL_RESTORE", restoreLog.getReason());
        assertEquals("ORDER", restoreLog.getReferenceType());
        assertEquals(orderId, restoreLog.getReferenceId());
        assertEquals("ADMIN", restoreLog.getOperatorType());
        assertEquals(1L, restoreLog.getOperatorId());
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

        JsonNode data = JsonUtil.readTree(loginResult.getResponse().getContentAsString()).path("data");
        return new LoginResult(data.path("id").asLong(), data.path("token").asText());
    }

    private String adminToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, 1L);
        return JwtUtil.createJWT(jwtProperties.getAdminSecretKey(), jwtProperties.getAdminTtl(), claims);
    }

    private void assertInventoryLog(ProductInventoryLog log, int changeQuantity, int stockBefore, int stockAfter,
                                    String reason, Long orderId, String operatorType) {
        assertEquals(changeQuantity, log.getChangeQuantity());
        assertEquals(stockBefore, log.getStockBefore());
        assertEquals(stockAfter, log.getStockAfter());
        assertEquals(reason, log.getReason());
        assertEquals("ORDER", log.getReferenceType());
        assertEquals(orderId, log.getReferenceId());
        assertEquals(operatorType, log.getOperatorType());
        assertEquals(loginResult.userId(), log.getOperatorId());
    }

    private void assertManualInventoryLog(ProductInventoryLog log, int changeQuantity, int stockBefore, int stockAfter,
                                          String remark) {
        assertEquals(changeQuantity, log.getChangeQuantity());
        assertEquals(stockBefore, log.getStockBefore());
        assertEquals(stockAfter, log.getStockAfter());
        assertEquals("MANUAL_ADJUSTMENT", log.getReason());
        assertEquals(remark, log.getRemark());
        assertEquals("ADMIN", log.getOperatorType());
        assertEquals(1L, log.getOperatorId());
    }
}
