package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.GiftBox;
import com.sky.result.Result;
import com.sky.service.GiftBoxService;
import com.sky.vo.ProductItemVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController("userGiftBoxController")
@RequestMapping("/user/giftbox")
@Api(tags = "C端-直送箱瀏覽接口")
public class GiftBoxController {
    @Autowired
    private GiftBoxService giftBoxService;

    @Resource(name = "appRedisTemplate")
    private RedisTemplate<String, Object> appRedisTemplate;

    /**
     * 条件查询
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询直送箱")
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
     * 根据直送箱id查询包含的單品列表
     *
     * @param id
     * @return
     */
    @GetMapping("/product/{id}")
    @ApiOperation("根据直送箱id查询包含的單品列表")
    public Result<List<ProductItemVO>> dishList(@PathVariable("id") Long id) {
        List<ProductItemVO> list = giftBoxService.getDishItemById(id);
        return Result.success(list);
    }
}
