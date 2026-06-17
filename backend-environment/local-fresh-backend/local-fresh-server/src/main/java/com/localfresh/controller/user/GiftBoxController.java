package com.localfresh.controller.user;

import com.localfresh.constant.StatusConstant;
import com.localfresh.entity.GiftBox;
import com.localfresh.result.Result;
import com.localfresh.service.GiftBoxService;
import com.localfresh.vo.ProductItemVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController("userGiftBoxController")
@RequestMapping("/user/giftbox")
@Tag(name = "會員端-直送箱瀏覽介面")
public class GiftBoxController {
    @Autowired
    private GiftBoxService giftBoxService;

    @Resource(name = "appRedisTemplate")
    private RedisTemplate<String, Object> appRedisTemplate;

    /**
     * 條件查詢
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @Operation(summary = "根據分類 ID 查詢直送箱")
    public Result<List<GiftBox>> list(Long categoryId) {
        String key = "giftbox_" + categoryId;

        List<GiftBox> cachedList = (List<GiftBox>) appRedisTemplate.opsForValue().get(key);
        if (cachedList != null && !cachedList.isEmpty()) {
            return Result.success(cachedList);
        }

        GiftBox setmeal = new GiftBox();
        setmeal.setCategoryId(categoryId);
        setmeal.setStatus(StatusConstant.ENABLE);

        List<GiftBox> list = giftBoxService.list(setmeal);
        appRedisTemplate.opsForValue().set(key, list, 30, TimeUnit.MINUTES);
        return Result.success(list);
    }

    /**
     * 根據直送箱 ID 查詢包含的商品列表
     *
     * @param id
     * @return
     */
    @GetMapping("/product/{id}")
    @Operation(summary = "根據直送箱 ID 查詢包含的商品列表")
    public Result<List<ProductItemVO>> dishList(@PathVariable("id") Long id) {
        List<ProductItemVO> list = giftBoxService.getDishItemById(id);
        return Result.success(list);
    }
}
