package com.localfresh.mapper;

import com.localfresh.entity.GiftBoxProduct;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GiftBoxProductMapper {
    /**
     * 根據菜品id查詢對應的套餐id
     * @param dishIds
     * @return
     */
    // select gift_box_id from gift_box_product where product_id in (1,2,3,4)
    List<Long> getSetmealIdsByDishIds(List<Long> dishIds);

    /**
     * 批量保存套餐和菜品的关联关系
     * @param giftBoxProducts
     */
    void insertBatch(List<GiftBoxProduct> giftBoxProducts);

    /**
     * 根据套餐id删除套餐和菜品的关联关系
     * @param giftBoxId
     */
    @Delete("delete from gift_box_product where gift_box_id = #{giftBoxId}")
    void deleteBySetmealId(Long giftBoxId);

    /**
     * 根据套餐id查询套餐和菜品的关联关系
     * @param giftBoxId
     * @return
     */
    @Select("select * from gift_box_product where gift_box_id = #{giftBoxId}")
    List<GiftBoxProduct> getBySetmealId(Long giftBoxId);



}
