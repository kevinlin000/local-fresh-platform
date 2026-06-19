package com.localfresh.service.payment;

import com.localfresh.constant.MessageConstant;
import com.localfresh.entity.Orders;
import com.localfresh.exception.OrderBusinessException;
import com.localfresh.utils.JsonUtil;
import com.localfresh.vo.OrderPaymentVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
@Slf4j
public class DemoPaymentGateway implements PaymentGateway {

    private static final String SIGNATURE_FIELD = "signature";

    @Value("${localfresh.payment.callback-secret:local-demo-payment-callback-secret}")
    private String callbackSecret;

    @Override
    public OrderPaymentVO createPaymentRequest(Orders order) {
        return OrderPaymentVO.builder()
                .packageStr("demo-paid:" + order.getNumber())
                .timeStamp(String.valueOf(System.currentTimeMillis() / 1000))
                .signType("DEMO")
                .build();
    }

    @Override
    public void refund(Orders order, String reason) {
        log.info("Demo refund recorded: orderNumber={}, amount={}, reason={}",
                order.getNumber(), order.getAmount(), reason);
    }

    @Override
    public String provider() {
        return "DEMO";
    }

    @Override
    public boolean completesPaymentOnRequest() {
        return true;
    }

    @Override
    public PaymentCallbackCommand parsePaymentCallback(Map<String, String> payload) {
        if (!isValidSignature(payload)) {
            throw new OrderBusinessException(MessageConstant.PAYMENT_CALLBACK_INVALID);
        }
        String orderNumber = payload.get("orderNumber");
        if (orderNumber == null || orderNumber.isBlank()) {
            throw new OrderBusinessException(MessageConstant.PAYMENT_CALLBACK_INVALID);
        }

        return PaymentCallbackCommand.builder()
                .provider(provider())
                .orderNumber(orderNumber)
                .providerReference(payload.get("providerReference"))
                .providerTradeNo(payload.get("providerTradeNo"))
                .rawPayload(JsonUtil.toJson(new TreeMap<>(payload)))
                .paymentSucceeded("SUCCESS".equals(payload.get("status")))
                .build();
    }

    private boolean isValidSignature(Map<String, String> payload) {
        String providedSignature = payload.get(SIGNATURE_FIELD);
        if (providedSignature == null || providedSignature.isBlank()) {
            return false;
        }
        String expectedSignature = sign(payload);
        return MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                providedSignature.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(Map<String, String> payload) {
        String canonicalPayload = new TreeMap<>(payload).entrySet().stream()
                .filter(entry -> !SIGNATURE_FIELD.equals(entry.getKey()))
                .map(entry -> entry.getKey() + "=" + (entry.getValue() == null ? "" : entry.getValue()))
                .collect(Collectors.joining("&"));
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(callbackSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(canonicalPayload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Demo payment callback signing failed", ex);
        }
    }
}
