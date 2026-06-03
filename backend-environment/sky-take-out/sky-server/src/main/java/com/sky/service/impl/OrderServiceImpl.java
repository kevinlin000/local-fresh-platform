package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.service.OrderCancellationService;
import com.sky.service.OrderPaymentService;
import com.sky.service.OrderQueryService;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.service.OrderSubmissionService;
import com.sky.service.support.OrderStatusTransitionPolicy;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.sky.service.support.OrderStatusTransitionPolicy.Transition;

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
    private WebSocketServer webSocketServer;

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
        Orders ordersDB = orderMapper.getById(ordersConfirmDTO.getId());
        OrderStatusTransitionPolicy.requireAllowed(ordersDB, Transition.ADMIN_CONFIRM);

        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();

        orderMapper.update(orders);
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
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        OrderStatusTransitionPolicy.requireAllowed(ordersDB, Transition.START_DELIVERY);

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为派送中
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);

        orderMapper.update(orders);
    }

    /**
     * 完成订单
     *
     * @param id
     */
    public void complete(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        OrderStatusTransitionPolicy.requireAllowed(ordersDB, Transition.COMPLETE_DELIVERY);

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为完成
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    /**
     * 客戶催單
     * @param id
     */
    public void reminder(Long id) {
        Orders ordersDB = orderMapper.getById(id);

        // 驗證訂單存在且屬於當前用戶
        if (ordersDB == null || !ordersDB.getUserId().equals(BaseContext.getCurrentId())) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 通過websocket向客戶端推送催單訊息
        Map map = new HashMap();
        map.put("type",2);  // 1表示來單提醒，2表示客戶催單
        map.put("orderId", id);
        map.put("content","訂單號：" + ordersDB.getNumber());

        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }

}
