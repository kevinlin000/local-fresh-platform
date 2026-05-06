package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Api(tags = "店舖相關接口")
@Slf4j
public class ShopController {

    public static final String KEY = "SHOP_STATUS";

    @Resource(name = "appRedisTemplate")
    private RedisTemplate<String, Object> appRedisTemplate;
    /**
     * 修改店舖營業狀態
     * @param status
     * @return
     */
    @PutMapping("/{status}")
    @ApiOperation("修改店舖營業狀態")
    public Result setStatus(@PathVariable Integer status){
        log.info("設置顛覆的營業狀態為：{}",status == 1 ? "營業中":"打烊中");
        appRedisTemplate.opsForValue().set(KEY,status);
        return Result.success();
    }

    /**
     * 獲取店舖營業狀態
     * @return
     */
    @GetMapping("/status")
    @ApiOperation("獲取店舖營業狀態")
    public Result<Integer> getStatus(){
        Integer status = (Integer) appRedisTemplate.opsForValue().get(KEY);
        String statusText = status == null ? "未設置" : (status == 1 ? "營業中" : "打烊中");
        log.info("獲取店舖的營業狀態為：{}", statusText);
        return Result.success(status);
    }


}
