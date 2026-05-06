package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Product;
import com.sky.result.Result;
import com.sky.service.ProductService;
import com.sky.vo.ProductVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController("userProductController")
@RequestMapping("/user/product")
@Slf4j
@Api(tags = "C端-單品瀏覽接口")
public class ProductController {
    @Autowired
    private ProductService productService;

    @Resource(name = "appRedisTemplate")
    private RedisTemplate<String, Object> appRedisTemplate;

    /**
     * 根据分类id查询單品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询單品")
    public Result<List<ProductVO>> list(Long categoryId) {

        // 構建redis的key,規則：dish_分類id
        String key = "dish_" + categoryId;

        // 查询redis中是否有数据
        List<ProductVO> list = (List<ProductVO>) appRedisTemplate.opsForValue().get(key);
        if(list != null && list.size() > 0){
            // 如果有数据，直接返回
            return Result.success(list);
        }

        Product dish = new Product();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);//查询起售中的菜品

        // 如果没有数据，查询数据库，并将数据存入redis
        list = productService.listWithFlavor(dish);
        appRedisTemplate.opsForValue().set(key, list);

        return Result.success(list);
    }

    /**
     * 根據id查詢商品詳情
     *
     * @param id 商品id
     * @return 商品詳情
     */
    @GetMapping("/{id}")
    @ApiOperation("根據id查詢商品詳情")
    public Result<ProductVO> getById(@PathVariable Long id) {
        ProductVO productVO = productService.getByIdWithFlavor(id);
        return Result.success(productVO);
    }

}
