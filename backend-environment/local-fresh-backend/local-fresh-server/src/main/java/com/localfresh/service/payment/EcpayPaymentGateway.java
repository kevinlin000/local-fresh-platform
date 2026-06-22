package com.localfresh.service.payment;

import com.localfresh.constant.MessageConstant;
import com.localfresh.entity.Orders;
import com.localfresh.exception.OrderBusinessException;
import com.localfresh.utils.HttpClientUtil;
import com.localfresh.utils.JsonUtil;
import com.localfresh.vo.OrderPaymentVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

@Component
@ConditionalOnProperty(name = "localfresh.payment.provider", havingValue = "ecpay")
@Slf4j
public class EcpayPaymentGateway implements PaymentGateway {

    private static final DateTimeFormatter ECPAY_DATE_TIME = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    @Value("${localfresh.payment.ecpay.merchant-id:}")
    private String merchantId;

    @Value("${localfresh.payment.ecpay.hash-key:}")
    private String hashKey;

    @Value("${localfresh.payment.ecpay.hash-iv:}")
    private String hashIv;

    @Value("${localfresh.payment.ecpay.checkout-url:https://payment-stage.ecpay.com.tw/Cashier/AioCheckOut/V5}")
    private String checkoutUrl;

    @Value("${localfresh.payment.ecpay.query-url:https://payment-stage.ecpay.com.tw/Cashier/QueryTradeInfo/V5}")
    private String queryUrl;

    @Value("${localfresh.payment.ecpay.return-url:}")
    private String returnUrl;

    @Value("${localfresh.payment.ecpay.order-result-url:}")
    private String orderResultUrl;

    @Autowired
    private EcpayCheckMacValueCalculator checkMacValueCalculator;

    @Override
    public OrderPaymentVO createPaymentRequest(Orders order) {
        requireConfigured();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("MerchantID", merchantId);
        params.put("MerchantTradeNo", order.getNumber());
        params.put("MerchantTradeDate", ECPAY_DATE_TIME.format(LocalDateTime.now()));
        params.put("PaymentType", "aio");
        params.put("TotalAmount", order.getAmount().setScale(0, java.math.RoundingMode.HALF_UP).toPlainString());
        params.put("TradeDesc", "Local Fresh order " + order.getNumber());
        params.put("ItemName", "Local Fresh Order " + order.getNumber());
        params.put("ReturnURL", returnUrl);
        params.put("ChoosePayment", "Credit");
        params.put("EncryptType", "1");
        if (orderResultUrl != null && !orderResultUrl.isBlank()) {
            params.put("OrderResultURL", orderResultUrl);
        }
        params.put("CheckMacValue", checkMacValueCalculator.calculate(params, hashKey, hashIv));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("checkoutUrl", checkoutUrl);
        request.put("params", params);
        return OrderPaymentVO.builder()
                .packageStr(JsonUtil.toJson(request))
                .paySign(params.get("CheckMacValue"))
                .signType("ECPAY_SHA256")
                .timeStamp(String.valueOf(System.currentTimeMillis() / 1000))
                .build();
    }

    @Override
    public void refund(Orders order, String reason) {
        log.info("ECPay refund is not automated in this portfolio demo: orderNumber={}, amount={}, reason={}",
                order.getNumber(), order.getAmount(), reason);
    }

    @Override
    public String provider() {
        return "ECPAY";
    }

    @Override
    public PaymentCallbackCommand parsePaymentCallback(Map<String, String> payload) {
        requireConfigured();
        if (!checkMacValueCalculator.verify(payload, hashKey, hashIv)) {
            throw new OrderBusinessException(MessageConstant.PAYMENT_CALLBACK_INVALID);
        }
        String orderNumber = payload.get("MerchantTradeNo");
        if (orderNumber == null || orderNumber.isBlank()) {
            throw new OrderBusinessException(MessageConstant.PAYMENT_CALLBACK_INVALID);
        }
        return PaymentCallbackCommand.builder()
                .provider(provider())
                .orderNumber(orderNumber)
                .providerReference(payload.get("PaymentDate"))
                .providerTradeNo(payload.get("TradeNo"))
                .rawPayload(JsonUtil.toJson(new TreeMap<>(payload)))
                .paymentSucceeded("1".equals(payload.get("RtnCode")))
                .build();
    }

    @Override
    public PaymentQueryResult queryPaymentStatus(String orderNumber) {
        requireConfigured();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("MerchantID", merchantId);
        params.put("MerchantTradeNo", orderNumber);
        params.put("TimeStamp", String.valueOf(System.currentTimeMillis() / 1000));
        params.put("CheckMacValue", checkMacValueCalculator.calculate(params, hashKey, hashIv));

        try {
            return parseTradeQueryResponse(HttpClientUtil.doPost(queryUrl, params));
        } catch (IOException ex) {
            throw new IllegalStateException("ECPay trade query failed", ex);
        }
    }

    PaymentQueryResult parseTradeQueryResponse(String responseBody) {
        Map<String, String> payload = parseFormUrlEncoded(responseBody);
        if (payload.containsKey("CheckMacValue") && !checkMacValueCalculator.verify(payload, hashKey, hashIv)) {
            throw new OrderBusinessException(MessageConstant.PAYMENT_CALLBACK_INVALID);
        }
        return PaymentQueryResult.builder()
                .provider(provider())
                .orderNumber(payload.get("MerchantTradeNo"))
                .status(resolveTradeStatus(payload.get("TradeStatus")))
                .providerReference(firstPresent(payload.get("PaymentDate"), payload.get("TradeDate"),
                        payload.get("TradeStatus")))
                .providerTradeNo(payload.get("TradeNo"))
                .rawPayload(JsonUtil.toJson(new TreeMap<>(payload)))
                .build();
    }

    private Map<String, String> parseFormUrlEncoded(String responseBody) {
        Map<String, String> payload = new LinkedHashMap<>();
        if (responseBody == null || responseBody.isBlank()) {
            return payload;
        }
        for (String pair : responseBody.split("&")) {
            int equalsIndex = pair.indexOf('=');
            if (equalsIndex < 0) {
                payload.put(urlDecode(pair), "");
            } else {
                payload.put(urlDecode(pair.substring(0, equalsIndex)), urlDecode(pair.substring(equalsIndex + 1)));
            }
        }
        return payload;
    }

    private PaymentQueryStatus resolveTradeStatus(String tradeStatus) {
        if ("1".equals(tradeStatus)) {
            return PaymentQueryStatus.SUCCEEDED;
        }
        if ("0".equals(tradeStatus)) {
            return PaymentQueryStatus.PENDING;
        }
        if (tradeStatus == null || tradeStatus.isBlank()) {
            return PaymentQueryStatus.UNKNOWN;
        }
        return PaymentQueryStatus.FAILED;
    }

    private String firstPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private void requireConfigured() {
        if (isBlank(merchantId) || isBlank(hashKey) || isBlank(hashIv) || isBlank(returnUrl)) {
            throw new OrderBusinessException(MessageConstant.PAYMENT_PROVIDER_NOT_CONFIGURED);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
