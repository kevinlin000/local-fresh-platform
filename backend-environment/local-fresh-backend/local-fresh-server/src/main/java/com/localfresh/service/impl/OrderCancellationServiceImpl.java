package com.localfresh.service.impl;

import com.localfresh.entity.GiftBoxProduct;
import com.localfresh.entity.OrderDetail;
import com.localfresh.entity.Orders;
import com.localfresh.mapper.GiftBoxProductMapper;
import com.localfresh.mapper.OrderDetailMapper;
import com.localfresh.mapper.OrderMapper;
import com.localfresh.mapper.ProductInventoryLogMapper;
import com.localfresh.service.BusinessMetricsService;
import com.localfresh.service.InventoryService;
import com.localfresh.service.OrderCancellationService;
import com.localfresh.service.payment.PaymentGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class OrderCancellationServiceImpl implements OrderCancellationService {

    private static final String INVENTORY_REASON_ORDER_CANCEL_RESTORE = "ORDER_CANCEL_RESTORE";
    private static final String INVENTORY_REFERENCE_ORDER = "ORDER";

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private GiftBoxProductMapper giftBoxProductMapper;

    @Autowired
    private ProductInventoryLogMapper productInventoryLogMapper;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private PaymentGateway paymentGateway;

    @Autowired
    private BusinessMetricsService businessMetricsService;

    @Override
    @Transactional
    public void cancelOrder(Orders ordersDB, String cancelReason, String rejectionReason,
                            String operatorType, Long operatorId) throws Exception {
        if (isCancellationAlreadyApplied(ordersDB.getId())) {
            log.info("Skip duplicate order cancellation: orderId={}", ordersDB.getId());
            businessMetricsService.recordOrderCancellation("duplicate");
            return;
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(cancelReason);
        orders.setRejectionReason(rejectionReason);
        orders.setCancelTime(LocalDateTime.now());

        if (Orders.PAID.equals(ordersDB.getPayStatus())) {
            paymentGateway.refund(ordersDB, cancelReason != null ? cancelReason : rejectionReason);
            orders.setPayStatus(Orders.REFUND);
        }

        orderMapper.update(orders);
        restoreProductStock(ordersDB.getId(), operatorType, operatorId);
        businessMetricsService.recordOrderCancellation("applied");
    }

    private boolean isCancellationAlreadyApplied(Long orderId) {
        Orders latestOrder = orderMapper.getById(orderId);
        if (latestOrder != null && Orders.CANCELLED.equals(latestOrder.getStatus())) {
            return true;
        }
        return productInventoryLogMapper.countByReferenceAndReason(INVENTORY_REFERENCE_ORDER, orderId,
                INVENTORY_REASON_ORDER_CANCEL_RESTORE) > 0;
    }

    private void restoreProductStock(Long orderId, String operatorType, Long operatorId) {
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderId);
        for (OrderDetail orderDetail : orderDetailList) {
            if (orderDetail.getProductId() != null && orderDetail.getNumber() != null && orderDetail.getNumber() > 0) {
                inventoryService.restoreProduct(orderDetail.getProductId(), orderDetail.getNumber(),
                        INVENTORY_REASON_ORDER_CANCEL_RESTORE, orderId, operatorType, operatorId);
            }
            if (orderDetail.getGiftBoxId() != null && orderDetail.getNumber() != null && orderDetail.getNumber() > 0) {
                restoreGiftBoxStock(orderDetail.getGiftBoxId(), orderDetail.getNumber(), orderId, operatorType, operatorId);
            }
        }
    }

    private void restoreGiftBoxStock(Long giftBoxId, Integer giftBoxQuantity, Long orderId, String operatorType, Long operatorId) {
        List<GiftBoxProduct> giftBoxProducts = giftBoxProductMapper.getBySetmealId(giftBoxId);
        for (GiftBoxProduct giftBoxProduct : giftBoxProducts) {
            int restoredQuantity = giftBoxQuantity * giftBoxProduct.getCopies();
            inventoryService.restoreProduct(giftBoxProduct.getProductId(), restoredQuantity,
                    INVENTORY_REASON_ORDER_CANCEL_RESTORE, orderId, operatorType, operatorId);
        }
    }
}
