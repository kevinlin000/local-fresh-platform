package com.localfresh.mapper;

import com.localfresh.entity.GiftBoxProduct;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GiftBoxProductMapper {
    /**
     * 根據商品id查詢對應的直送箱id
     * @param dishIds
     * @return
     */
    // select gift_box_id from gift_box_product where product_id in (1,2,3,4)
    List<Long> getSetmealIdsByDishIds(List<Long> dishIds);

    /**
     * 批次保存直送箱和商品的關聯關係
     * @param giftBoxProducts
     */
    void insertBatch(List<GiftBoxProduct> giftBoxProducts);

    /**
     * 根據直送箱id刪除直送箱和商品的關聯關係
     * @param giftBoxId
     */
    @Delete("delete from gift_box_product where gift_box_id = #{giftBoxId}")
    void deleteBySetmealId(Long giftBoxId);

    /**
     * 根據直送箱id查詢直送箱和商品的關聯關係
     * @param giftBoxId
     * @return
     */
    @Select("select * from gift_box_product where gift_box_id = #{giftBoxId}")
    List<GiftBoxProduct> getBySetmealId(Long giftBoxId);



}
