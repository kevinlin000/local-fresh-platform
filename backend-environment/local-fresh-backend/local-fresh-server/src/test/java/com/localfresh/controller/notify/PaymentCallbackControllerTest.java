package com.localfresh.controller.notify;

import com.localfresh.constant.MessageConstant;
import com.localfresh.exception.OrderBusinessException;
import com.localfresh.service.OrderPaymentService;
import com.localfresh.service.payment.PaymentCallbackCommand;
import com.localfresh.service.payment.PaymentGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentCallbackControllerTest {

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private OrderPaymentService orderPaymentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PaymentCallbackController controller = new PaymentCallbackController();
        ReflectionTestUtils.setField(controller, "paymentGateway", paymentGateway);
        ReflectionTestUtils.setField(controller, "orderPaymentService", orderPaymentService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void callbackShouldReturnOkAfterVerifiedProviderPayload() throws Exception {
        PaymentCallbackCommand command = PaymentCallbackCommand.builder()
                .provider("DEMO")
                .orderNumber("ORDER-CB-001")
                .paymentSucceeded(true)
                .build();
        when(paymentGateway.parsePaymentCallback(ArgumentMatchers.<Map<String, String>>any())).thenReturn(command);

        mockMvc.perform(post("/payment/callback")
                        .param("orderNumber", "ORDER-CB-001")
                        .param("status", "SUCCESS")
                        .param("signature", "signature"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(TEXT_PLAIN))
                .andExpect(content().string("1|OK"));

        verify(orderPaymentService).handlePaymentCallback(command);
    }

    @Test
    void callbackShouldReturnFailWhenProviderRejectsSignature() throws Exception {
        when(paymentGateway.parsePaymentCallback(ArgumentMatchers.<Map<String, String>>any()))
                .thenThrow(new OrderBusinessException(MessageConstant.PAYMENT_CALLBACK_INVALID));

        mockMvc.perform(post("/payment/callback")
                        .param("orderNumber", "ORDER-CB-001")
                        .param("signature", "bad-signature"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(TEXT_PLAIN))
                .andExpect(content().string("0|FAIL"));

        verify(orderPaymentService, never()).handlePaymentCallback(any());
    }
}
