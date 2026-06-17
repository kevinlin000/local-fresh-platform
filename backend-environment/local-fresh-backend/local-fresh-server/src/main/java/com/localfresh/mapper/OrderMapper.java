package com.localfresh.mapper;


import com.github.pagehelper.Page;
import com.localfresh.dto.GoodsSalesDTO;
import com.localfresh.dto.OrdersPageQueryDTO;
import com.localfresh.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {
    /**
     * 插入訂單數據
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 根據訂單号查詢訂單
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改訂單信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 依訂單號更新付款後的訂單狀態。
     */
    @Update("update orders set status = #{orderStatus},pay_status = #{orderPaidStatus} ,checkout_time = #{check_out_time} " +
            "where number = #{orderNumber}")
    void updateStatus(Integer orderStatus, Integer orderPaidStatus, LocalDateTime check_out_time, String orderNumber);

    @Update("update orders set status = #{toStatus}, pay_status = #{toPayStatus}, checkout_time = #{checkoutTime} " +
            "where number = #{orderNumber} and status = #{fromStatus} and pay_status = #{fromPayStatus}")
    int markPaymentSucceededByNumber(@Param("orderNumber") String orderNumber,
                                     @Param("fromStatus") Integer fromStatus,
                                     @Param("fromPayStatus") Integer fromPayStatus,
                                     @Param("toStatus") Integer toStatus,
                                     @Param("toPayStatus") Integer toPayStatus,
                                     @Param("checkoutTime") LocalDateTime checkoutTime);

    /**
     * 分頁條件查詢并按下單時间排序
     * @param ordersPageQueryDTO
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根據id查詢訂單
     * @param id
     */
    @Select("select * from orders where id=#{id}")
    Orders getById(Long id);

    /**
     * 根據狀態统计訂單數量
     * @param status
     */
    @Select("select count(id) from orders where status = #{status}")
    Integer countStatus(Integer status);

    /**
     * 根據訂單狀態和下單時間來查詢訂單
     * @param status
     * @param orderTime
     * @return
     */
    @Select("select * from orders where status = #{status} and order_time < #{orderTime}")
    List<Orders> getByStatusAndOrderTimeLT(Integer status, LocalDateTime orderTime);

    /**
     * 根據動態條件來統計營業額數據
     * @param map
     * @return
     */
    Double sumByMap(Map map);

    /**
     * 根據動態條件統計訂單數量
     * @param map
     * @return
     */
    Integer countByMap(Map map);

    /**
     * 統計指定時間區間內的銷量top10排名
     * @param begin
     * @param end
     * @return
     */
    List<GoodsSalesDTO> getSalesTop10(LocalDateTime begin, LocalDateTime end);

    void updateStatusBatch(@Param("ids") List<Long> ids,
                           @Param("fromStatus") Integer fromStatus,
                           @Param("toStatus") Integer toStatus);

    int cancelPendingGroupOrder(@Param("id") Long id,
                                @Param("fromStatus") Integer fromStatus,
                                @Param("toStatus") Integer toStatus,
                                @Param("cancelReason") String cancelReason,
                                @Param("cancelTime") LocalDateTime cancelTime);
}
