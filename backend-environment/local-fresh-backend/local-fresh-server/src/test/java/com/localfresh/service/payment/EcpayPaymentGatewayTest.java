package com.localfresh.service.payment;

import com.localfresh.constant.MessageConstant;
import com.localfresh.entity.Orders;
import com.localfresh.exception.OrderBusinessException;
import com.localfresh.utils.JsonUtil;
import com.localfresh.vo.OrderPaymentVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EcpayPaymentGatewayTest {

    private static final String MERCHANT_ID = "2000132";
    private static final String HASH_KEY = "5294y06JbISpM5x9";
    private static final String HASH_IV = "v77hoKGq4kWxNNIS";

    private EcpayCheckMacValueCalculator calculator;
    private EcpayPaymentGateway gateway;

    @BeforeEach
    void setUp() {
        calculator = new EcpayCheckMacValueCalculator();
        gateway = new EcpayPaymentGateway();
        ReflectionTestUtils.setField(gateway, "merchantId", MERCHANT_ID);
        ReflectionTestUtils.setField(gateway, "hashKey", HASH_KEY);
        ReflectionTestUtils.setField(gateway, "hashIv", HASH_IV);
        ReflectionTestUtils.setField(gateway, "checkoutUrl", "https://payment-stage.ecpay.com.tw/Cashier/AioCheckOut/V5");
        ReflectionTestUtils.setField(gateway, "returnUrl", "http://localhost:8080/payment/callback");
        ReflectionTestUtils.setField(gateway, "orderResultUrl", "http://localhost:5173/orders");
        ReflectionTestUtils.setField(gateway, "checkMacValueCalculator", calculator);
    }

    @Test
    void createPaymentRequestShouldBuildSignedRedirectPayload() {
        Orders order = new Orders();
        order.setNumber("ORDER1001");
        order.setAmount(new BigDecimal("560.00"));

        OrderPaymentVO paymentRequest = gateway.createPaymentRequest(order);

        assertEquals("ECPAY_SHA256", paymentRequest.getSignType());
        assertEquals(paymentRequest.getPaySign(), JsonUtil.readTree(paymentRequest.getPackageStr())
                .path("params").path("CheckMacValue").asText());
        assertEquals("ORDER1001", JsonUtil.readTree(paymentRequest.getPackageStr())
                .path("params").path("MerchantTradeNo").asText());
    }

    @Test
    void parsePaymentCallbackShouldVerifyCheckMacValueAndMapEcpayFields() {
        Map<String, String> payload = successCallback();

        PaymentCallbackCommand command = gateway.parsePaymentCallback(payload);

        assertEquals("ECPAY", command.getProvider());
        assertEquals("ORDER1001", command.getOrderNumber());
        assertEquals("250620000000001", command.getProviderTradeNo());
        assertEquals("2026/06/20 03:20:00", command.getProviderReference());
        assertTrue(command.isPaymentSucceeded());
        assertTrue(command.getRawPayload().contains("\"MerchantTradeNo\":\"ORDER1001\""));
    }

    @Test
    void parsePaymentCallbackShouldMapFailedRtnCodeWithoutMarkingSuccess() {
        Map<String, String> payload = successCallback();
        payload.put("RtnCode", "10100073");
        payload.put("RtnMsg", "Failed");
        payload.put("CheckMacValue", calculator.calculate(payload, HASH_KEY, HASH_IV));

        PaymentCallbackCommand command = gateway.parsePaymentCallback(payload);

        assertFalse(command.isPaymentSucceeded());
        assertEquals("ORDER1001", command.getOrderNumber());
    }

    @Test
    void parsePaymentCallbackShouldRejectInvalidCheckMacValue() {
        Map<String, String> payload = successCallback();
        payload.put("CheckMacValue", "BAD");

        OrderBusinessException exception = assertThrows(OrderBusinessException.class,
                () -> gateway.parsePaymentCallback(payload));

        assertEquals(MessageConstant.PAYMENT_CALLBACK_INVALID, exception.getMessage());
    }

    @Test
    void parseTradeQueryResponseShouldMapSucceededTradeStatus() {
        Map<String, String> payload = tradeQueryPayload("1");

        PaymentQueryResult result = gateway.parseTradeQueryResponse(toQueryString(payload));

        assertEquals("ECPAY", result.getProvider());
        assertEquals("ORDER1001", result.getOrderNumber());
        assertEquals(PaymentQueryStatus.SUCCEEDED, result.getStatus());
        assertEquals("250620000000001", result.getProviderTradeNo());
        assertEquals("2026/06/20 03:20:00", result.getProviderReference());
        assertTrue(result.getRawPayload().contains("\"MerchantTradeNo\":\"ORDER1001\""));
    }

    @Test
    void parseTradeQueryResponseShouldKeepTradeStatusZeroPending() {
        Map<String, String> payload = tradeQueryPayload("0");

        PaymentQueryResult result = gateway.parseTradeQueryResponse(toQueryString(payload));

        assertEquals(PaymentQueryStatus.PENDING, result.getStatus());
    }

    @Test
    void parseTradeQueryResponseShouldTreatOtherTradeStatusAsFailed() {
        Map<String, String> payload = tradeQueryPayload("10200095");

        PaymentQueryResult result = gateway.parseTradeQueryResponse(toQueryString(payload));

        assertEquals(PaymentQueryStatus.FAILED, result.getStatus());
    }

    @Test
    void createPaymentRequestShouldFailWhenProviderIsNotConfigured() {
        ReflectionTestUtils.setField(gateway, "returnUrl", "");
        Orders order = new Orders();
        order.setNumber("ORDER1001");
        order.setAmount(new BigDecimal("560.00"));

        OrderBusinessException exception = assertThrows(OrderBusinessException.class,
                () -> gateway.createPaymentRequest(order));

        assertEquals(MessageConstant.PAYMENT_PROVIDER_NOT_CONFIGURED, exception.getMessage());
    }

    private Map<String, String> successCallback() {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("MerchantID", MERCHANT_ID);
        payload.put("MerchantTradeNo", "ORDER1001");
        payload.put("RtnCode", "1");
        payload.put("RtnMsg", "Succeeded");
        payload.put("TradeNo", "250620000000001");
        payload.put("TradeAmt", "560");
        payload.put("PaymentDate", "2026/06/20 03:20:00");
        payload.put("PaymentType", "Credit_CreditCard");
        payload.put("CheckMacValue", calculator.calculate(payload, HASH_KEY, HASH_IV));
        return payload;
    }

    private Map<String, String> tradeQueryPayload(String tradeStatus) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("MerchantID", MERCHANT_ID);
        payload.put("MerchantTradeNo", "ORDER1001");
        payload.put("TradeNo", "250620000000001");
        payload.put("TradeAmt", "560");
        payload.put("TradeDate", "2026/06/20 03:19:30");
        payload.put("PaymentDate", "2026/06/20 03:20:00");
        payload.put("TradeStatus", tradeStatus);
        payload.put("CheckMacValue", calculator.calculate(payload, HASH_KEY, HASH_IV));
        return payload;
    }

    private String toQueryString(Map<String, String> payload) {
        return payload.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue().replace(" ", "%20"))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }
}
