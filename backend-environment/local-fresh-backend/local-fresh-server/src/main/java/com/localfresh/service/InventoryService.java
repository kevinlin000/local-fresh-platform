package com.localfresh.service;

public interface InventoryService {

    void reserveProduct(Long productId, Integer quantity, String reason,
                        Long referenceId, String operatorType, Long operatorId);

    void restoreProduct(Long productId, Integer quantity, String reason,
                        Long referenceId, String operatorType, Long operatorId);

    void adjustProduct(Long productId, Integer changeQuantity, String reason, String remark,
                       String operatorType, Long operatorId);
}
