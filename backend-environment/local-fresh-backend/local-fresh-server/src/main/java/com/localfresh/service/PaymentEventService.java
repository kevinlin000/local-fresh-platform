package com.localfresh.service;

import com.localfresh.dto.PaymentEventPageQueryDTO;
import com.localfresh.result.PageResult;

public interface PaymentEventService {

    PageResult pageQuery(PaymentEventPageQueryDTO queryDTO);
}
