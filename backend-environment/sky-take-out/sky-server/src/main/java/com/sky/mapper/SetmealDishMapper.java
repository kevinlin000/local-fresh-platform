package com.sky.mapper;

import com.sky.entity.GiftBoxProduct;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {
    /**
     * 根據菜品id查詢對應的套餐id
     * @param dishIds
     * @return
     */
    //select setmeal id from setmeal dish where dish id in (1,2,3,4)
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
    @Delete("delete from setmeal_dish where setmeal_id = #{giftBoxId}")
    void deleteBySetmealId(Long giftBoxId);

    /**
     * 根据套餐id查询套餐和菜品的关联关系
     * @param giftBoxId
     * @return
     */
    @Select("select * from setmeal_dish where setmeal_id = #{giftBoxId}")
    List<GiftBoxProduct> getBySetmealId(Long giftBoxId);



}
