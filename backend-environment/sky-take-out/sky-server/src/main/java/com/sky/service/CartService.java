package com.sky.service;

import com.sky.dto.CartDTO;
import com.sky.entity.Cart;

import java.util.List;

public interface CartService {
    /**
     * 添加購物車
     * @param shoppingCartDTO
     */
    void addShoppingCart(CartDTO shoppingCartDTO);

    /**
     * 減少購物車中商品數量，減到0時自動刪除
     *
     * @param cartDTO 購物車商品資訊
     */
    void subShoppingCart(CartDTO cartDTO);

    /**
     * 查看購物車
     * @return
     */
    List<Cart> showShoppingCart();

    /**
     * 清空購物車
     */
    void cleanShoppingCart();
}
