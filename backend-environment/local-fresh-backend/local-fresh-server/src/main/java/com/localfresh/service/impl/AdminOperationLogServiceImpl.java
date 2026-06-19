package com.localfresh.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.localfresh.context.BaseContext;
import com.localfresh.dto.AdminOperationLogPageQueryDTO;
import com.localfresh.entity.AdminOperationLog;
import com.localfresh.entity.Orders;
import com.localfresh.mapper.AdminOperationLogMapper;
import com.localfresh.result.PageResult;
import com.localfresh.service.AdminOperationLogService;
import com.localfresh.vo.AdminOperationLogVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminOperationLogServiceImpl implements AdminOperationLogService {

    private static final String TARGET_TYPE_ORDER = "ORDER";
    private static final String TARGET_TYPE_PRODUCT = "PRODUCT";
    private static final String OPERATOR_TYPE_ADMIN = "ADMIN";
    private static final String ACTION_PRODUCT_INVENTORY_ADJUST = "PRODUCT_INVENTORY_ADJUST";

    @Autowired
    private AdminOperationLogMapper adminOperationLogMapper;

    @Override
    public void recordOrderAction(String action, Orders order, Integer afterStatus, String reason) {
        AdminOperationLog log = AdminOperationLog.builder()
                .action(action)
                .targetType(TARGET_TYPE_ORDER)
                .targetId(order.getId())
                .beforeValue(String.valueOf(order.getStatus()))
                .afterValue(String.valueOf(afterStatus))
                .reason(reason)
                .operatorType(OPERATOR_TYPE_ADMIN)
                .operatorId(BaseContext.getCurrentId())
                .createdAt(LocalDateTime.now())
                .build();

        adminOperationLogMapper.insert(log);
    }

    @Override
    public void recordProductInventoryAdjustment(Long productId, Integer stockBefore, Integer stockAfter, String reason) {
        AdminOperationLog log = AdminOperationLog.builder()
                .action(ACTION_PRODUCT_INVENTORY_ADJUST)
                .targetType(TARGET_TYPE_PRODUCT)
                .targetId(productId)
                .beforeValue(String.valueOf(stockBefore))
                .afterValue(String.valueOf(stockAfter))
                .reason(reason)
                .operatorType(OPERATOR_TYPE_ADMIN)
                .operatorId(BaseContext.getCurrentId())
                .createdAt(LocalDateTime.now())
                .build();

        adminOperationLogMapper.insert(log);
    }

    @Override
    public PageResult pageQuery(AdminOperationLogPageQueryDTO queryDTO) {
        PageHelper.startPage(queryDTO.getPage(), queryDTO.getPageSize());
        Page<AdminOperationLog> page = adminOperationLogMapper.pageQuery(queryDTO);
        List<AdminOperationLogVO> records = page.getResult().stream()
                .map(this::toVO)
                .toList();
        return new PageResult(page.getTotal(), records);
    }

    private AdminOperationLogVO toVO(AdminOperationLog log) {
        AdminOperationLogVO vo = new AdminOperationLogVO();
        BeanUtils.copyProperties(log, vo);
        return vo;
    }
}
