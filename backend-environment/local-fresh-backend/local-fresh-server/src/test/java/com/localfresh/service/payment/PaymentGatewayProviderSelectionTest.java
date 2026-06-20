package com.localfresh.service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.localfresh.entity.Orders;
import com.localfresh.utils.JsonUtil;
import com.localfresh.vo.OrderPaymentVO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentGatewayProviderSelectionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(DemoPaymentGateway.class, EcpayPaymentGateway.class,
                    EcpayCheckMacValueCalculator.class);

    @Test
    void defaultProviderShouldRemainDemoForPortfolioDemos() {
        contextRunner.run(context -> {
            PaymentGateway gateway = context.getBean(PaymentGateway.class);

            assertInstanceOf(DemoPaymentGateway.class, gateway);
            assertEquals("DEMO", gateway.provider());
            assertTrue(gateway.completesPaymentOnRequest());
        });
    }

    @Test
    void ecpayProviderShouldBuildSignedCheckoutPayloadFromConfiguredUrls() {
        contextRunner
                .withPropertyValues(
                        "localfresh.payment.provider=ecpay",
                        "localfresh.payment.ecpay.merchant-id=2000132",
                        "localfresh.payment.ecpay.hash-key=5294y06JbISpM5x9",
                        "localfresh.payment.ecpay.hash-iv=v77hoKGq4kWxNNIS",
                        "localfresh.payment.ecpay.checkout-url=https://payment-stage.ecpay.com.tw/Cashier/AioCheckOut/V5",
                        "localfresh.payment.ecpay.return-url=https://localfresh-demo.duckdns.org/payment/callback",
                        "localfresh.payment.ecpay.order-result-url=https://d3hqnux25iirgl.cloudfront.net/orders")
                .run(context -> {
                    PaymentGateway gateway = context.getBean(PaymentGateway.class);

                    assertInstanceOf(EcpayPaymentGateway.class, gateway);
                    assertEquals("ECPAY", gateway.provider());
                    assertFalse(gateway.completesPaymentOnRequest());

                    Orders order = new Orders();
                    order.setNumber("ORDER-ECPAY-READY");
                    order.setAmount(new BigDecimal("560.00"));

                    OrderPaymentVO paymentRequest = gateway.createPaymentRequest(order);
                    JsonNode checkoutPackage = JsonUtil.readTree(paymentRequest.getPackageStr());
                    JsonNode params = checkoutPackage.path("params");

                    assertEquals("ECPAY_SHA256", paymentRequest.getSignType());
                    assertEquals("https://payment-stage.ecpay.com.tw/Cashier/AioCheckOut/V5",
                            checkoutPackage.path("checkoutUrl").asText());
                    assertEquals("2000132", params.path("MerchantID").asText());
                    assertEquals("ORDER-ECPAY-READY", params.path("MerchantTradeNo").asText());
                    assertEquals("https://localfresh-demo.duckdns.org/payment/callback",
                            params.path("ReturnURL").asText());
                    assertEquals("https://d3hqnux25iirgl.cloudfront.net/orders",
                            params.path("OrderResultURL").asText());
                    assertEquals("Credit", params.path("ChoosePayment").asText());
                    assertEquals("1", params.path("EncryptType").asText());
                    assertEquals(params.path("CheckMacValue").asText(), paymentRequest.getPaySign());
                    assertFalse(paymentRequest.getPaySign().isBlank());
                });
    }
}
