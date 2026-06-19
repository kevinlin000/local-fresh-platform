package com.localfresh.service.payment;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EcpayCheckMacValueCalculatorTest {

    private final EcpayCheckMacValueCalculator calculator = new EcpayCheckMacValueCalculator();

    @Test
    void calculateShouldIgnoreInputOrderAndExistingCheckMacValue() {
        Map<String, String> first = new LinkedHashMap<>();
        first.put("MerchantTradeNo", "ORDER1001");
        first.put("MerchantID", "2000132");
        first.put("RtnCode", "1");
        first.put("TradeNo", "250620000000001");
        first.put("CheckMacValue", "STALE");

        Map<String, String> second = new LinkedHashMap<>();
        second.put("TradeNo", "250620000000001");
        second.put("RtnCode", "1");
        second.put("MerchantID", "2000132");
        second.put("MerchantTradeNo", "ORDER1001");

        String firstCheckMacValue = calculator.calculate(first, "5294y06JbISpM5x9", "v77hoKGq4kWxNNIS");
        String secondCheckMacValue = calculator.calculate(second, "5294y06JbISpM5x9", "v77hoKGq4kWxNNIS");

        assertEquals(firstCheckMacValue, secondCheckMacValue);
        assertEquals(firstCheckMacValue.toUpperCase(java.util.Locale.ROOT), firstCheckMacValue);
    }

    @Test
    void verifyShouldAcceptCaseInsensitiveCheckMacValue() {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("MerchantID", "2000132");
        payload.put("MerchantTradeNo", "ORDER1001");
        payload.put("RtnCode", "1");
        payload.put("TradeNo", "250620000000001");
        payload.put("CheckMacValue", calculator.calculate(payload, "5294y06JbISpM5x9", "v77hoKGq4kWxNNIS")
                .toLowerCase(java.util.Locale.ROOT));

        assertTrue(calculator.verify(payload, "5294y06JbISpM5x9", "v77hoKGq4kWxNNIS"));

        payload.put("CheckMacValue", "BAD");
        assertFalse(calculator.verify(payload, "5294y06JbISpM5x9", "v77hoKGq4kWxNNIS"));
    }
}
