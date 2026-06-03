package com.sky.service;

import com.sky.vo.BusinessDataVO;
import com.sky.vo.ProductOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.GiftBoxOverViewVO;
import com.sky.vo.ProductVO;
import java.time.LocalDateTime;
import java.util.List;

public interface WorkspaceService {

    /**
     * 根据时间段统计营业数据
     * @param begin
     * @param end
     * @return
     */
    BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end);

    /**
     * 查询订单管理数据
     * @return
     */
    OrderOverViewVO getOrderOverView();

    /**
     * 查询菜品总览
     * @return
     */
    ProductOverViewVO getDishOverView();

    /**
     * 查询套餐总览
     * @return
     */
    GiftBoxOverViewVO getSetmealOverView();

    /**
     * 查詢低庫存商品
     * @return 低庫存商品
     */
    List<ProductVO> listLowStockProducts();

}
