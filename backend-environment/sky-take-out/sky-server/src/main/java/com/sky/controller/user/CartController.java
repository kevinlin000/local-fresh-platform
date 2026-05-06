package com.sky.controller.user;

import com.sky.dto.CartDTO;
import com.sky.entity.Cart;
import com.sky.result.Result;
import com.sky.service.CartService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/user/cart")
@Slf4j
@Api(tags = "會員端購物車介面")
public class CartController {

    @Autowired
    private CartService cartService;
    /**
     * 加入購物車
     * @param shoppingCartDTO
     * @return
     */
    @PostMapping("/add")
    @ApiOperation("加入購物車")
    public Result add(@RequestBody CartDTO shoppingCartDTO) {
        log.info("加入購物車, 商品資訊為: {}", shoppingCartDTO);
        cartService.addShoppingCart(shoppingCartDTO);
        return Result.success();
    }

    /**
     * 減少購物車中的商品數量，減到0時自動刪除
     *
     * @param cartDTO 購物車商品資訊
     * @return success
     */
    @PostMapping("/sub")
    @ApiOperation("減少購物車商品數量")
    public Result sub(@RequestBody CartDTO cartDTO) {
        log.info("減少購物車商品, 商品資訊為: {}", cartDTO);
        cartService.subShoppingCart(cartDTO);
        return Result.success();
    }

    /**
     * 查詢購物車
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("查詢購物車")
    public Result<List<Cart>> list() {
        List<Cart> list = cartService.showShoppingCart();
        return Result.success(list);

    }

    /**
     * 清空購物車
     * @return
     */
    @DeleteMapping("/clean")
    @ApiOperation("清空購物車")
    public Result clean(){
        cartService.cleanShoppingCart();
        return  Result.success();
    }

}
