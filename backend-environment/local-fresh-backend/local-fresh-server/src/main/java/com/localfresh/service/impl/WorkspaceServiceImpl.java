package com.localfresh.service.impl;

import com.localfresh.constant.StatusConstant;
import com.localfresh.entity.Orders;
import com.localfresh.mapper.ProductMapper;
import com.localfresh.mapper.OrderMapper;
import com.localfresh.mapper.GiftBoxMapper;
import com.localfresh.mapper.MemberMapper;
import com.localfresh.service.WorkspaceService;
import com.localfresh.vo.BusinessDataVO;
import com.localfresh.vo.ProductOverViewVO;
import com.localfresh.vo.OrderOverViewVO;
import com.localfresh.vo.GiftBoxOverViewVO;
import com.localfresh.vo.ProductVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private MemberMapper memberMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private GiftBoxMapper giftBoxMapper;

    /**
     * 根據時间段统计营业資料
     * @param begin
     * @param end
     * @return
     */
    public BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end) {
        /**
         * 營業額：当日已完成訂單的總金額
         * 有效訂單：当日已完成訂單的數量
         * 訂單完成率：有效訂單數 / 總訂單数
         * 平均客單價：營業額 / 有效訂單數
         * 新增會員：当日新增會員的數量
         */

        Map map = new HashMap();
        map.put("begin",begin);
        map.put("end",end);

        //查詢總訂單数
        Integer totalOrderCount = orderMapper.countByMap(map);

        map.put("status", Orders.COMPLETED);
        //營業額
        Double turnover = orderMapper.sumByMap(map);
        turnover = turnover == null? 0.0 : turnover;

        //有效訂單數
        Integer validOrderCount = orderMapper.countByMap(map);

        Double unitPrice = 0.0;

        Double orderCompletionRate = 0.0;
        if(totalOrderCount != 0 && validOrderCount != 0){
            //訂單完成率
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
            //平均客單價
            unitPrice = turnover / validOrderCount;
        }

        //新增會員數
        Integer newUsers = memberMapper.countByMap(map);

        return BusinessDataVO.builder()
                .turnover(turnover)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(newUsers)
                .build();
    }


    /**
     * 查詢訂單管理資料
     *
     * @return
     */
    public OrderOverViewVO getOrderOverView() {
        Map map = new HashMap();
        map.put("begin", LocalDateTime.now().with(LocalTime.MIN));
        map.put("status", Orders.TO_BE_CONFIRMED);

        //待確認
        Integer waitingOrders = orderMapper.countByMap(map);

        //待配送
        map.put("status", Orders.CONFIRMED);
        Integer deliveredOrders = orderMapper.countByMap(map);

        //已完成
        map.put("status", Orders.COMPLETED);
        Integer completedOrders = orderMapper.countByMap(map);

        //已取消
        map.put("status", Orders.CANCELLED);
        Integer cancelledOrders = orderMapper.countByMap(map);

        //全部訂單
        map.put("status", null);
        Integer allOrders = orderMapper.countByMap(map);

        return OrderOverViewVO.builder()
                .waitingOrders(waitingOrders)
                .deliveredOrders(deliveredOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .allOrders(allOrders)
                .build();
    }

    /**
     * 查詢商品總览
     *
     * @return
     */
    public ProductOverViewVO getDishOverView() {
        Map map = new HashMap();
        map.put("status", StatusConstant.ENABLE);
        Integer sold = productMapper.countByMap(map);

        map.put("status", StatusConstant.DISABLE);
        Integer discontinued = productMapper.countByMap(map);
        Integer lowStock = productMapper.countLowStock();

        return ProductOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .lowStock(lowStock)
                .build();
    }

    /**
     * 查詢直送箱總览
     *
     * @return
     */
    public GiftBoxOverViewVO getSetmealOverView() {
        Map map = new HashMap();
        map.put("status", StatusConstant.ENABLE);
        Integer sold = giftBoxMapper.countByMap(map);

        map.put("status", StatusConstant.DISABLE);
        Integer discontinued = giftBoxMapper.countByMap(map);

        return GiftBoxOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }

    @Override
    public List<ProductVO> listLowStockProducts() {
        return productMapper.listLowStock();
    }
}
