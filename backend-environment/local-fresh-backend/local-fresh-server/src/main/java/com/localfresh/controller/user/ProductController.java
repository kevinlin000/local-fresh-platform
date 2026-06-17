package com.localfresh.controller.user;

import com.localfresh.constant.StatusConstant;
import com.localfresh.entity.Product;
import com.localfresh.result.Result;
import com.localfresh.service.ProductService;
import com.localfresh.vo.ProductVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController("userProductController")
@RequestMapping("/user/product")
@Slf4j
@Tag(name = "會員端-單品瀏覽介面")
public class ProductController {
    @Autowired
    private ProductService productService;

    @Resource(name = "appRedisTemplate")
    private RedisTemplate<String, Object> appRedisTemplate;

    /**
     * 根據分類 ID 查詢單品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @Operation(summary = "查詢可販售單品")
    @SuppressWarnings("unchecked")
    public Result<List<ProductVO>> list(@RequestParam(required = false) Long categoryId,
                                        @RequestParam(required = false) String productName) {
        String normalizedProductName = normalizeProductName(productName);
        boolean cacheable = !StringUtils.hasText(normalizedProductName);

        String key = "product_" + (categoryId == null ? "all" : categoryId);

        if (cacheable) {
            // 分類瀏覽可以快取；關鍵字搜尋不快取，避免低命中率查詢污染 Redis。
            List<ProductVO> cachedList = (List<ProductVO>) appRedisTemplate.opsForValue().get(key);
            if (cachedList != null) {
                return Result.success(cachedList);
            }
        }

        Product dish = new Product();
        dish.setCategoryId(categoryId);
        dish.setProductName(normalizedProductName);
        dish.setStatus(StatusConstant.ENABLE);//查詢起售中的商品

        List<ProductVO> list = productService.listWithFlavor(dish);
        if (cacheable) {
            appRedisTemplate.opsForValue().set(key, list, 30, TimeUnit.MINUTES);
        }

        return Result.success(list);
    }

    /**
     * 根據 ID 查詢商品詳情
     *
     * @param id 商品id
     * @return 商品詳情
     */
    @GetMapping("/{id}")
    @Operation(summary = "根據 ID 查詢商品詳情")
    public Result<ProductVO> getById(@PathVariable Long id) {
        ProductVO productVO = productService.getByIdWithFlavor(id);
        return Result.success(productVO);
    }

    private String normalizeProductName(String productName) {
        if (!StringUtils.hasText(productName)) {
            return null;
        }
        return productName.trim();
    }

}
