package com.localfresh.service.payment;

import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
public class EcpayCheckMacValueCalculator {

    private static final String CHECK_MAC_VALUE = "CheckMacValue";

    public String calculate(Map<String, String> payload, String hashKey, String hashIv) {
        TreeMap<String, String> sortedPayload = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        payload.forEach((key, value) -> {
            if (!CHECK_MAC_VALUE.equalsIgnoreCase(key)) {
                sortedPayload.put(key, value == null ? "" : value);
            }
        });

        String joined = sortedPayload.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&", "HashKey=" + hashKey + "&", "&HashIV=" + hashIv));
        String encoded = ecpayUrlEncode(joined).toLowerCase(Locale.ROOT);
        return sha256Hex(encoded).toUpperCase(Locale.ROOT);
    }

    public boolean verify(Map<String, String> payload, String hashKey, String hashIv) {
        String provided = payload.get(CHECK_MAC_VALUE);
        if (provided == null || provided.isBlank()) {
            return false;
        }
        String expected = calculate(payload, hashKey, hashIv);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.toUpperCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
    }

    private String ecpayUrlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("%2D", "-")
                .replace("%5F", "_")
                .replace("%2E", ".")
                .replace("%21", "!")
                .replace("%2A", "*")
                .replace("%28", "(")
                .replace("%29", ")");
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("ECPay CheckMacValue calculation failed", ex);
        }
    }
}
