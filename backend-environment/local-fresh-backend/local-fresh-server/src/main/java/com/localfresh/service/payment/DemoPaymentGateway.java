package com.localfresh.service.payment;

import com.localfresh.entity.Orders;
import com.localfresh.vo.OrderPaymentVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DemoPaymentGateway implements PaymentGateway {

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
}
