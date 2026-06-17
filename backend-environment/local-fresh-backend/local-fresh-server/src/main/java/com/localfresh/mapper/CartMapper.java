package com.localfresh.mapper;

import com.localfresh.entity.Cart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CartMapper {

    /**
     * 動態條件查詢
     * @param shoppingCart
     * @return
     */
    List<Cart> list(Cart shoppingCart);

    /**
     * 根據ID修改商品數量
     * @param shoppingCart
     */
    @Update("update cart set number = #{number} where id = #{id}")
    void updateNumberById(Cart shoppingCart);

    /**
     * 插入購物車數據
     * @param shoppingCart
     */
    @Insert("insert into cart(name, user_id, product_id, gift_box_id, product_spec, number, amount, image, create_time) " +
            "values(#{name}, #{userId}, #{productId}, #{giftBoxId}, #{productSpec}, #{number}, #{amount}, #{image},#{createTime})")
    void insert(Cart shoppingCart);


    /**
     * 根據用戶ID刪除購物車數據
     * @param userId
     */
    @Delete("delete from cart where user_id = #{userId}")
    void deleteByUserId(Long userId);

    /**
     * 根據購物車ID刪除單筆資料
     *
     * @param id 購物車ID
     */
    @Delete("delete from cart where id = #{id}")
    void deleteById(Long id);

    /**
     * 批次插入购物车資料
     *
     * @param shoppingCartList
     */
    void insertBatch(List<Cart> shoppingCartList);
}
