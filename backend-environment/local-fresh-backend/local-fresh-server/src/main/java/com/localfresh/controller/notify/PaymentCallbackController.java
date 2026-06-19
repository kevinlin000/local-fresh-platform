package com.localfresh.controller.notify;

import com.localfresh.exception.OrderBusinessException;
import com.localfresh.service.OrderPaymentService;
import com.localfresh.service.payment.PaymentCallbackCommand;
import com.localfresh.service.payment.PaymentGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/payment/callback")
@Slf4j
public class PaymentCallbackController {

    @Autowired
    private PaymentGateway paymentGateway;

    @Autowired
    private OrderPaymentService orderPaymentService;

    @PostMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public String callback(@RequestParam Map<String, String> payload) {
        try {
            PaymentCallbackCommand command = paymentGateway.parsePaymentCallback(payload);
            orderPaymentService.handlePaymentCallback(command);
            return "1|OK";
        } catch (OrderBusinessException | UnsupportedOperationException ex) {
            log.warn("Payment callback rejected: {}", ex.getMessage());
            return "0|FAIL";
        }
    }
}
