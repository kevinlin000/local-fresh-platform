package com.localfresh.service.payment;

import com.localfresh.constant.MessageConstant;
import com.localfresh.entity.Orders;
import com.localfresh.exception.OrderBusinessException;
import com.localfresh.utils.JsonUtil;
import com.localfresh.vo.OrderPaymentVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

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

    private void requireConfigured() {
        if (isBlank(merchantId) || isBlank(hashKey) || isBlank(hashIv) || isBlank(returnUrl)) {
            throw new OrderBusinessException(MessageConstant.PAYMENT_PROVIDER_NOT_CONFIGURED);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
