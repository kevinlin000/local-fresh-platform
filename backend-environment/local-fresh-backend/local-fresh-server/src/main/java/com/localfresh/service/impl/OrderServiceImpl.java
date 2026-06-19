package com.localfresh.service.impl;

import com.localfresh.constant.MessageConstant;
import com.localfresh.context.BaseContext;
import com.localfresh.dto.*;
import com.localfresh.entity.*;
import com.localfresh.exception.OrderBusinessException;
import com.localfresh.mapper.*;
import com.localfresh.service.AdminOperationLogService;
import com.localfresh.service.OrderCancellationService;
import com.localfresh.service.OrderFulfillmentService;
import com.localfresh.service.OrderPaymentService;
import com.localfresh.service.OrderQueryService;
import com.localfresh.result.PageResult;
import com.localfresh.service.OrderService;
import com.localfresh.service.OrderSubmissionService;
import com.localfresh.service.support.OrderStatusTransitionPolicy;
import com.localfresh.vo.OrderPaymentVO;
import com.localfresh.vo.OrderStatisticsVO;
import com.localfresh.vo.OrderSubmitVO;
import com.localfresh.vo.OrderVO;
import com.localfresh.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.localfresh.service.support.OrderStatusTransitionPolicy.Transition;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private static final String INVENTORY_OPERATOR_MEMBER = "MEMBER";
    private static final String INVENTORY_OPERATOR_ADMIN = "ADMIN";
    private static final String ACTION_ORDER_REJECT = "ORDER_REJECT";
    private static final String ACTION_ORDER_CANCEL = "ORDER_CANCEL";

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private OrderCancellationService orderCancellationService;

    @Autowired
    private OrderPaymentService orderPaymentService;

    @Autowired
    private OrderQueryService orderQueryService;

    @Autowired
    private OrderSubmissionService orderSubmissionService;

    @Autowired
    private OrderFulfillmentService orderFulfillmentService;

    @Autowired
    private AdminOperationLogService adminOperationLogService;

    /**
     * 用戶下單
     * @param ordersSubmitDTO
     * @return
     */
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        return orderSubmissionService.submitOrder(ordersSubmitDTO);
    }

    /**
     * 訂單支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 目前登入會員id
        Long userId = BaseContext.getCurrentId();
        Orders ordersDB = orderMapper.getByNumber(ordersPaymentDTO.getOrderNumber());
        if (ordersDB == null || !userId.equals(ordersDB.getUserId())) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (!Orders.UN_PAID.equals(ordersDB.getPayStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        OrderStatusTransitionPolicy.requireAllowed(ordersDB, Transition.PAY);

        return orderPaymentService.requestPayment(ordersDB);
    }

    /**
     * 付款成功，修改訂單狀態
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        orderPaymentService.handlePaymentSuccess(outTradeNo);
    }

    /**
     * 會員端訂單分頁查詢
     *
     * @param pageNum
     * @param pageSize
     * @param status
     * @return
     */
    public PageResult pageQuery4User(int pageNum, int pageSize, Integer status) {
        return orderQueryService.pageQuery4User(pageNum, pageSize, status);
    }

    /**
     * 查詢訂單明細（管理端，不驗證所有權）
     *
     * @param id
     * @return
     */
    public OrderVO details(Long id) {
        return orderQueryService.details(id);
    }

    /**
     * 查詢訂單詳情（用戶端，驗證訂單所有權）
     *
     * @param id
     * @return
     */
    public OrderVO userDetails(Long id) {
        return orderQueryService.userDetails(id);
    }

    /**
     * 會員取消訂單
     *
     * @param id
     */
    public void userCancelById(Long id) throws Exception {
        // 根據 id 查詢訂單
        Orders ordersDB = orderMapper.getById(id);

        // 驗證訂單是否存在
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (!ordersDB.getUserId().equals(BaseContext.getCurrentId())) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        OrderStatusTransitionPolicy.requireAllowed(ordersDB, Transition.USER_CANCEL);

        orderCancellationService.cancelOrder(ordersDB, "會員取消", null,
                INVENTORY_OPERATOR_MEMBER, BaseContext.getCurrentId());
    }

    /**
     * 再下一單
     *
     * @param id
     */
    public void repetition(Long id) {
        Long userId = BaseContext.getCurrentId();

        // 驗證訂單所有權
        Orders ordersDB = orderMapper.getById(id);
        if (ordersDB == null || !ordersDB.getUserId().equals(userId)) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 根據訂單 id 查詢目前訂單明細
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 將訂單明細物件轉換為購物車物件
        List<Cart> shoppingCartList = orderDetailList.stream().map(x -> {
            Cart shoppingCart = new Cart();

            // 將原訂單明細內的商品資訊重新複製到購物車物件中
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

        // 将購物車物件批次添加到資料库
        cartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 訂單搜尋
     *
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        return orderQueryService.conditionSearch(ordersPageQueryDTO);
    }

    /**
     * 各個狀態的訂單數量統計
     *
     * @return
     */
    public OrderStatisticsVO statistics() {
        return orderQueryService.statistics();
    }


    /**
     * 確認訂單
     *
     * @param ordersConfirmDTO
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        orderFulfillmentService.confirm(ordersConfirmDTO);
    }

    /**
     * 拒絕訂單
     *
     * @param ordersRejectionDTO
     */
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        // 根據id查詢訂單
        Orders ordersDB = orderMapper.getById(ordersRejectionDTO.getId());

        OrderStatusTransitionPolicy.requireAllowed(ordersDB, Transition.ADMIN_REJECT);

        orderCancellationService.cancelOrder(ordersDB, null, ordersRejectionDTO.getRejectionReason(),
                INVENTORY_OPERATOR_ADMIN, BaseContext.getCurrentId());
        adminOperationLogService.recordOrderAction(ACTION_ORDER_REJECT, ordersDB, Orders.CANCELLED,
                ordersRejectionDTO.getRejectionReason());
    }

    /**
     * 取消訂單
     *
     * @param ordersCancelDTO
     */
    public void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception {
        Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());

        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        OrderStatusTransitionPolicy.requireAllowed(ordersDB, Transition.ADMIN_CANCEL);

        orderCancellationService.cancelOrder(ordersDB, ordersCancelDTO.getCancelReason(), null,
                INVENTORY_OPERATOR_ADMIN, BaseContext.getCurrentId());
        adminOperationLogService.recordOrderAction(ACTION_ORDER_CANCEL, ordersDB, Orders.CANCELLED,
                ordersCancelDTO.getCancelReason());
    }

    /**
     * 配送訂單
     *
     * @param id
     */
    public void delivery(Long id) {
        orderFulfillmentService.delivery(id);
    }

    /**
     * 完成訂單
     *
     * @param id
     */
    public void complete(Long id) {
        orderFulfillmentService.complete(id);
    }

    /**
     * 客戶催單
     * @param id
     */
    public void reminder(Long id) {
        orderFulfillmentService.reminder(id);
    }

}
