package com.localfresh.service;

import com.localfresh.dto.*;
import com.localfresh.result.PageResult;
import com.localfresh.vo.OrderPaymentVO;
import com.localfresh.vo.OrderStatisticsVO;
import com.localfresh.vo.OrderSubmitVO;
import com.localfresh.vo.OrderVO;

public interface OrderService {

    /**
     * 用戶下單
     * @param ordersSubmitDTO
     * @return
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 訂單支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 付款成功，修改訂單狀態
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    /**
     * 會員端訂單分頁查詢
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    PageResult pageQuery4User(int page, int pageSize, Integer status);

    /**
     * 查詢訂單详情（管理端，不做所有權驗證）
     * @param id
     * @return
     */
    OrderVO details(Long id);

    /**
     * 查詢訂單詳情（用戶端，驗證訂單所有權）
     * @param id
     * @return
     */
    OrderVO userDetails(Long id);

    /**
     * 會員取消訂單
     * @param id
     */
    void userCancelById(Long id) throws Exception;

    /**
     * 再来一单
     *
     * @param id
     */
    void repetition(Long id);

    /**
     * 條件搜索訂單
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 各個狀態的訂單數量统计
     * @return
     */
    OrderStatisticsVO statistics();

    /**
     * 接单
     *
     * @param ordersConfirmDTO
     */
    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    /**
     * 拒单
     *
     * @param ordersRejectionDTO
     */
    void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception;

    /**
     * 商家取消訂單
     *
     * @param ordersCancelDTO
     */
    void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception;

    /**
     * 配送訂單
     *
     * @param id
     */
    void delivery(Long id);

    /**
     * 完成訂單
     *
     * @param id
     */
    void complete(Long id);


    /**
     * 客戶催單
     * @param id
     */
    void reminder(Long id);
}
