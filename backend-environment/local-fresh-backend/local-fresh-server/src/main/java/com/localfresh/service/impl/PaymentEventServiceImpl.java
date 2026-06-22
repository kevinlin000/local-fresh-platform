package com.localfresh.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.localfresh.dto.PaymentEventPageQueryDTO;
import com.localfresh.entity.PaymentEvent;
import com.localfresh.mapper.PaymentEventMapper;
import com.localfresh.result.PageResult;
import com.localfresh.service.PaymentEventService;
import com.localfresh.vo.PaymentEventVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentEventServiceImpl implements PaymentEventService {

    @Autowired
    private PaymentEventMapper paymentEventMapper;

    @Override
    public PageResult pageQuery(PaymentEventPageQueryDTO queryDTO) {
        PageHelper.startPage(queryDTO.getPage(), queryDTO.getPageSize());
        Page<PaymentEvent> page = paymentEventMapper.pageQuery(queryDTO);
        List<PaymentEventVO> records = page.getResult().stream()
                .map(this::toVO)
                .toList();
        return new PageResult(page.getTotal(), records);
    }

    @Override
    public PageResult pagePendingRequestsWithoutTerminalCallback(PaymentEventPageQueryDTO queryDTO) {
        PageHelper.startPage(queryDTO.getPage(), queryDTO.getPageSize());
        Page<PaymentEvent> page = paymentEventMapper.pagePendingRequestsWithoutTerminalCallback(queryDTO);
        List<PaymentEventVO> records = page.getResult().stream()
                .map(this::toVO)
                .toList();
        return new PageResult(page.getTotal(), records);
    }

    private PaymentEventVO toVO(PaymentEvent paymentEvent) {
        PaymentEventVO vo = new PaymentEventVO();
        BeanUtils.copyProperties(paymentEvent, vo);
        return vo;
    }
}
