package com.localfresh.controller.notify;

import com.localfresh.exception.OrderBusinessException;
import com.localfresh.service.OrderPaymentService;
import com.localfresh.service.payment.PaymentCallbackCommand;
import com.localfresh.service.payment.PaymentGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

    @PostMapping
    public ResponseEntity<String> callback(@RequestParam Map<String, String> payload) {
        try {
            PaymentCallbackCommand command = paymentGateway.parsePaymentCallback(payload);
            orderPaymentService.handlePaymentCallback(command);
            return ResponseEntity.ok("1|OK");
        } catch (OrderBusinessException | UnsupportedOperationException ex) {
            log.warn("Payment callback rejected: {}", ex.getMessage());
            return ResponseEntity.ok("0|FAIL");
        }
    }
}
