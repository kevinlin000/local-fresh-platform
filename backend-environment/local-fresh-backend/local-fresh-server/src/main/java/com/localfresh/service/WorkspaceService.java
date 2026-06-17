package com.localfresh.service;

import com.localfresh.vo.BusinessDataVO;
import com.localfresh.vo.ProductOverViewVO;
import com.localfresh.vo.OrderOverViewVO;
import com.localfresh.vo.GiftBoxOverViewVO;
import com.localfresh.vo.ProductVO;
import java.time.LocalDateTime;
import java.util.List;

public interface WorkspaceService {

    /**
     * 根據時間區間統計營業資料
     * @param begin
     * @param end
     * @return
     */
    BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end);

    /**
     * 查詢訂單管理資料
     * @return
     */
    OrderOverViewVO getOrderOverView();

    /**
     * 查詢商品總覽
     * @return
     */
    ProductOverViewVO getDishOverView();

    /**
     * 查詢直送箱總覽
     * @return
     */
    GiftBoxOverViewVO getSetmealOverView();

    /**
     * 查詢低庫存商品
     * @return 低庫存商品
     */
    List<ProductVO> listLowStockProducts();

}
