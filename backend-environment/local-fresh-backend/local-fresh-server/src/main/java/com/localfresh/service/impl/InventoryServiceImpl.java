package com.localfresh.service.impl;

import com.localfresh.constant.MessageConstant;
import com.localfresh.entity.Product;
import com.localfresh.entity.ProductInventoryLog;
import com.localfresh.exception.BaseException;
import com.localfresh.exception.OrderBusinessException;
import com.localfresh.mapper.ProductInventoryLogMapper;
import com.localfresh.mapper.ProductMapper;
import com.localfresh.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class InventoryServiceImpl implements InventoryService {

    private static final String INVENTORY_REFERENCE_ORDER = "ORDER";

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductInventoryLogMapper productInventoryLogMapper;

    @Override
    public void reserveProduct(Long productId, Integer quantity, String reason,
                               Long referenceId, String operatorType, Long operatorId) {
        if (productId == null || quantity == null || quantity <= 0) {
            throw new OrderBusinessException(MessageConstant.PRODUCT_STOCK_NOT_ENOUGH);
        }
        if (productMapper.getById(productId) == null) {
            throw new OrderBusinessException(MessageConstant.PRODUCT_STOCK_NOT_ENOUGH);
        }

        int updatedRows = productMapper.decreaseStock(productId, quantity);
        if (updatedRows == 0) {
            throw new OrderBusinessException(MessageConstant.PRODUCT_STOCK_NOT_ENOUGH);
        }

        int stockAfter = productMapper.getById(productId).getStock();
        int stockBefore = stockAfter + quantity;
        writeInventoryLog(productId, -quantity, stockBefore, stockAfter,
                reason, referenceId, null, operatorType, operatorId);
    }

    @Override
    public void restoreProduct(Long productId, Integer quantity, String reason,
                               Long referenceId, String operatorType, Long operatorId) {
        if (productId == null || quantity == null || quantity <= 0) {
            return;
        }
        if (productMapper.getById(productId) == null) {
            return;
        }

        productMapper.increaseStock(productId, quantity);
        int stockAfter = productMapper.getById(productId).getStock();
        int stockBefore = stockAfter - quantity;
        writeInventoryLog(productId, quantity, stockBefore, stockAfter,
                reason, referenceId, null, operatorType, operatorId);
    }

    @Override
    public void adjustProduct(Long productId, Integer changeQuantity, String reason, String remark,
                              String operatorType, Long operatorId) {
        Product product = productMapper.getById(productId);
        if (product == null) {
            throw new BaseException(MessageConstant.PRODUCT_NOT_AVAILABLE);
        }
        if (changeQuantity == null || changeQuantity == 0) {
            throw new BaseException("庫存異動量不能為 0");
        }

        int updatedRows = productMapper.adjustStock(productId, changeQuantity);
        if (updatedRows == 0) {
            throw new BaseException(MessageConstant.PRODUCT_STOCK_NOT_ENOUGH);
        }

        int stockAfter = productMapper.getById(productId).getStock();
        int stockBefore = stockAfter - changeQuantity;
        writeInventoryLog(productId, changeQuantity, stockBefore, stockAfter,
                reason, null, remark, operatorType, operatorId);
    }

    private void writeInventoryLog(Long productId, Integer changeQuantity, Integer stockBefore, Integer stockAfter,
                                   String reason, Long referenceId, String remark,
                                   String operatorType, Long operatorId) {
        ProductInventoryLog.ProductInventoryLogBuilder builder = ProductInventoryLog.builder()
                .productId(productId)
                .changeQuantity(changeQuantity)
                .stockBefore(stockBefore)
                .stockAfter(stockAfter)
                .reason(reason)
                .remark(remark)
                .operatorType(operatorType)
                .operatorId(operatorId)
                .createdAt(LocalDateTime.now());

        if (referenceId != null) {
            builder.referenceType(INVENTORY_REFERENCE_ORDER)
                    .referenceId(referenceId);
        }

        productInventoryLogMapper.insert(builder.build());
    }
}
