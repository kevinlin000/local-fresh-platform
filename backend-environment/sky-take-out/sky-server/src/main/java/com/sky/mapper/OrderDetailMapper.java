package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderDetailMapper {
    /**
     * 批量插入訂單明細
     * @param orderDetailList
     */
    void insertBatch(List<OrderDetail> orderDetailList);

    @Insert("insert into order_detail (name, image, order_id, product_id, gift_box_id, product_spec, number, amount) " +
            "values (#{name},#{image},#{orderId},#{productId},#{giftBoxId},#{productSpec},#{number},#{amount})")
    void insert(OrderDetail orderDetail);

    /**
     * 根据订单id查询订单明细
     * @param orderId
     * @return
     */
    @Select("select * from order_detail where order_id = #{orderId}")
    List<OrderDetail> getByOrderId(Long orderId);
}
