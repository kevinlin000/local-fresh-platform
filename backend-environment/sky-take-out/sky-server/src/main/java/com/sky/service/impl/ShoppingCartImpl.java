package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.CartDTO;
import com.sky.entity.Product;
import com.sky.entity.GiftBox;
import com.sky.entity.Cart;
import com.sky.mapper.ProductMapper;
import com.sky.mapper.GiftBoxMapper;
import com.sky.mapper.CartMapper;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ShoppingCartImpl implements ShoppingCartService {

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private GiftBoxMapper giftBoxMapper;

    /**
     * 添加購物車
     * @param shoppingCartDTO
     */
    public void addShoppingCart(CartDTO shoppingCartDTO) {

        //判斷當前加入到購物車中的商品是否已經存在了
        Cart shoppingCart = new Cart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);

        List<Cart> list = cartMapper.list(shoppingCart);

        //如果已經存在了，只需要將數量加一
        if (list != null && list.size() > 0) {
            Cart cart = list.get(0);
            cart.setNumber(cart.getNumber() + 1);//update shopping_cart set number = ? where id = ?
            cartMapper.updateNumberById(cart);
        }else{
            //如果不存在，需要插入一條購物車數據

            //判斷本次添加到購物車的是菜品還是套餐
            Long productId = shoppingCartDTO.getProductId();
            if (productId != null) {
                //本次添加到購物車的是菜品
                Product dish = productMapper.getById(productId);
                shoppingCart.setName(dish.getProductName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());

            }else{
                //本次添加到購物車的是套餐
                Long giftBoxId = shoppingCart.getGiftBoxId();

                GiftBox setmeal = giftBoxMapper.getById(giftBoxId);
                shoppingCart.setName(setmeal.getBoxName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());

            }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());

            cartMapper.insert(shoppingCart);
        }

    }

    /**
     * 查看購物車
     * @return
     */

    public List<Cart> showShoppingCart() {
        //獲取當前用戶的id
        Long userId = BaseContext.getCurrentId();
        Cart shoppingCart = Cart.builder()
                .userId(userId)
                .build();
        List<Cart> list = cartMapper.list(shoppingCart);
        return list;
    }

    /**
     * 清空購物車
     */
    public void cleanShoppingCart() {
        //獲取當前用戶的id
        Long userId = BaseContext.getCurrentId();
        cartMapper.deleteByUserId(userId);

    }
}
