package com.localfresh.service.payment;

import com.localfresh.constant.MessageConstant;
import com.localfresh.exception.OrderBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoPaymentGatewayTest {

    private static final String SECRET = "test-callback-secret";

    private DemoPaymentGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new DemoPaymentGateway();
        ReflectionTestUtils.setField(gateway, "callbackSecret", SECRET);
    }

    @Test
    void parsePaymentCallbackShouldVerifySignatureAndMapPayload() {
        Map<String, String> payload = validPayload("SUCCESS");

        PaymentCallbackCommand command = gateway.parsePaymentCallback(payload);

        assertEquals("DEMO", command.getProvider());
        assertEquals("ORDER-CB-001", command.getOrderNumber());
        assertEquals("demo-callback:ORDER-CB-001", command.getProviderReference());
        assertEquals("DEMO-TRADE-001", command.getProviderTradeNo());
        assertTrue(command.isPaymentSucceeded());
        assertTrue(command.getRawPayload().contains("\"orderNumber\":\"ORDER-CB-001\""));
    }

    @Test
    void parsePaymentCallbackShouldMapFailedProviderStatusWithoutMarkingSuccess() {
        Map<String, String> payload = validPayload("FAILED");

        PaymentCallbackCommand command = gateway.parsePaymentCallback(payload);

        assertFalse(command.isPaymentSucceeded());
        assertEquals("ORDER-CB-001", command.getOrderNumber());
    }

    @Test
    void parsePaymentCallbackShouldRejectInvalidSignature() {
        Map<String, String> payload = validPayload("SUCCESS");
        payload.put("signature", "bad-signature");

        OrderBusinessException exception = assertThrows(OrderBusinessException.class,
                () -> gateway.parsePaymentCallback(payload));

        assertEquals(MessageConstant.PAYMENT_CALLBACK_INVALID, exception.getMessage());
    }

    private static Map<String, String> validPayload(String status) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("orderNumber", "ORDER-CB-001");
        payload.put("providerReference", "demo-callback:ORDER-CB-001");
        payload.put("providerTradeNo", "DEMO-TRADE-001");
        payload.put("status", status);
        payload.put("signature", sign(payload));
        return payload;
    }

    private static String sign(Map<String, String> payload) {
        String canonicalPayload = new TreeMap<>(payload).entrySet().stream()
                .filter(entry -> !"signature".equals(entry.getKey()))
                .map(entry -> entry.getKey() + "=" + (entry.getValue() == null ? "" : entry.getValue()))
                .collect(Collectors.joining("&"));
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(canonicalPayload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
