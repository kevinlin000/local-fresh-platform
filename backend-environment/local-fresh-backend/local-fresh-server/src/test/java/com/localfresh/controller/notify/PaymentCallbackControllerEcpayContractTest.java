package com.localfresh.controller.notify;

import com.localfresh.service.OrderPaymentService;
import com.localfresh.service.payment.EcpayCheckMacValueCalculator;
import com.localfresh.service.payment.EcpayPaymentGateway;
import com.localfresh.service.payment.PaymentCallbackCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentCallbackControllerEcpayContractTest {

    private static final String MERCHANT_ID = "2000132";
    private static final String HASH_KEY = "5294y06JbISpM5x9";
    private static final String HASH_IV = "v77hoKGq4kWxNNIS";

    @Mock
    private OrderPaymentService orderPaymentService;

    private EcpayCheckMacValueCalculator calculator;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        calculator = new EcpayCheckMacValueCalculator();

        EcpayPaymentGateway gateway = new EcpayPaymentGateway();
        ReflectionTestUtils.setField(gateway, "merchantId", MERCHANT_ID);
        ReflectionTestUtils.setField(gateway, "hashKey", HASH_KEY);
        ReflectionTestUtils.setField(gateway, "hashIv", HASH_IV);
        ReflectionTestUtils.setField(gateway, "returnUrl", "https://localfresh-demo.duckdns.org/payment/callback");
        ReflectionTestUtils.setField(gateway, "checkMacValueCalculator", calculator);

        PaymentCallbackController controller = new PaymentCallbackController();
        ReflectionTestUtils.setField(controller, "paymentGateway", gateway);
        ReflectionTestUtils.setField(controller, "orderPaymentService", orderPaymentService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void signedEcpayCallbackShouldReachOrderPaymentService() throws Exception {
        Map<String, String> payload = signedSuccessPayload();

        mockMvc.perform(post("/payment/callback")
                        .contentType(APPLICATION_FORM_URLENCODED)
                        .params(org.springframework.util.CollectionUtils.toMultiValueMap(toListValueMap(payload))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(TEXT_PLAIN))
                .andExpect(content().string("1|OK"));

        ArgumentCaptor<PaymentCallbackCommand> commandCaptor = ArgumentCaptor.forClass(PaymentCallbackCommand.class);
        verify(orderPaymentService).handlePaymentCallback(commandCaptor.capture());
        PaymentCallbackCommand command = commandCaptor.getValue();
        assertEquals("ECPAY", command.getProvider());
        assertEquals("ORDER-ECPAY-CB-001", command.getOrderNumber());
        assertEquals("250620000000001", command.getProviderTradeNo());
        assertEquals("2026/06/20 12:00:00", command.getProviderReference());
        assertTrue(command.isPaymentSucceeded());
        assertTrue(command.getRawPayload().contains("\"MerchantTradeNo\":\"ORDER-ECPAY-CB-001\""));
    }

    @Test
    void invalidEcpaySignatureShouldReturnFailWithoutCallingPaymentService() throws Exception {
        Map<String, String> payload = signedSuccessPayload();
        payload.put("CheckMacValue", "BAD");

        mockMvc.perform(post("/payment/callback")
                        .contentType(APPLICATION_FORM_URLENCODED)
                        .params(org.springframework.util.CollectionUtils.toMultiValueMap(toListValueMap(payload))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(TEXT_PLAIN))
                .andExpect(content().string("0|FAIL"));

        verify(orderPaymentService, never()).handlePaymentCallback(org.mockito.ArgumentMatchers.any());
    }

    private Map<String, String> signedSuccessPayload() {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("MerchantID", MERCHANT_ID);
        payload.put("MerchantTradeNo", "ORDER-ECPAY-CB-001");
        payload.put("RtnCode", "1");
        payload.put("RtnMsg", "Succeeded");
        payload.put("TradeNo", "250620000000001");
        payload.put("TradeAmt", "560");
        payload.put("PaymentDate", "2026/06/20 12:00:00");
        payload.put("PaymentType", "Credit_CreditCard");
        payload.put("CheckMacValue", calculator.calculate(payload, HASH_KEY, HASH_IV));
        return payload;
    }

    private Map<String, java.util.List<String>> toListValueMap(Map<String, String> payload) {
        Map<String, java.util.List<String>> result = new LinkedHashMap<>();
        payload.forEach((key, value) -> result.put(key, java.util.List.of(value)));
        return result;
    }
}
