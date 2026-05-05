package com.sky.mapper;

import com.sky.entity.Cart;
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
    @Update("update shopping_cart set number = #{number} where id = #{id}")
    void updateNumberById(Cart shoppingCart);

    /**
     * 插入購物車數據
     * @param shoppingCart
     */
    @Insert("insert into shopping_cart(name, user_id, dish_id, setmeal_id, dish_flavor, number, amount, image, create_time) " +
            "values(#{name}, #{userId}, #{productId}, #{giftBoxId}, #{productSpec}, #{number}, #{amount}, #{image},#{createTime})")
    void insert(Cart shoppingCart);


    /**
     * 根據用戶ID刪除購物車數據
     * @param userId
     */
    @Delete("delete from shopping_cart where user_id = #{userId}")
    void deleteByUserId(Long userId);

    /**
     * 批量插入购物车数据
     *
     * @param shoppingCartList
     */
    void insertBatch(List<Cart> shoppingCartList);
}
