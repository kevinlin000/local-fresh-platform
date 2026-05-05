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
     * 查看購物車
     * @return
     */
    List<Cart> showShoppingCart();

    /**
     * 清空購物車
     */
    void cleanShoppingCart();
}
