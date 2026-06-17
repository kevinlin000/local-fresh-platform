package com.localfresh.service.impl;

import com.localfresh.constant.MessageConstant;
import com.localfresh.context.BaseContext;
import com.localfresh.dto.*;
import com.localfresh.entity.*;
import com.localfresh.exception.OrderBusinessException;
import com.localfresh.mapper.*;
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

    /**
     * 用戶下單
     * @param ordersSubmitDTO
     * @return
     */
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        return orderSubmissionService.submitOrder(ordersSubmitDTO);
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
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
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        orderPaymentService.handlePaymentSuccess(outTradeNo);
    }

    /**
     * 用户端订单分页查询
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
     * 查询订单详情（管理端，不驗證所有權）
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
     * 用户取消订单
     *
     * @param id
     */
    public void userCancelById(Long id) throws Exception {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (!ordersDB.getUserId().equals(BaseContext.getCurrentId())) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        OrderStatusTransitionPolicy.requireAllowed(ordersDB, Transition.USER_CANCEL);

        orderCancellationService.cancelOrder(ordersDB, "用户取消", null,
                INVENTORY_OPERATOR_MEMBER, BaseContext.getCurrentId());
    }

    /**
     * 再来一单
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

        // 根据订单id查询当前订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 将订单详情对象转换为购物车对象
        List<Cart> shoppingCartList = orderDetailList.stream().map(x -> {
            Cart shoppingCart = new Cart();

            // 将原订单详情里面的菜品信息重新复制到购物车对象中
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

        // 将购物车对象批量添加到数据库
        cartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 订单搜索
     *
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        return orderQueryService.conditionSearch(ordersPageQueryDTO);
    }

    /**
     * 各个状态的订单数量统计
     *
     * @return
     */
    public OrderStatisticsVO statistics() {
        return orderQueryService.statistics();
    }


    /**
     * 接单
     *
     * @param ordersConfirmDTO
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        orderFulfillmentService.confirm(ordersConfirmDTO);
    }

    /**
     * 拒单
     *
     * @param ordersRejectionDTO
     */
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(ordersRejectionDTO.getId());

        OrderStatusTransitionPolicy.requireAllowed(ordersDB, Transition.ADMIN_REJECT);

        orderCancellationService.cancelOrder(ordersDB, null, ordersRejectionDTO.getRejectionReason(),
                INVENTORY_OPERATOR_ADMIN, BaseContext.getCurrentId());
    }

    /**
     * 取消订单
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
    }

    /**
     * 派送订单
     *
     * @param id
     */
    public void delivery(Long id) {
        orderFulfillmentService.delivery(id);
    }

    /**
     * 完成订单
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
