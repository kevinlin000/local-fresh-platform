package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Tag(name = "店舖相關介面")
@Slf4j
public class ShopController {

    public static final String KEY = "SHOP_STATUS";

    @Resource(name = "appRedisTemplate")
    private RedisTemplate<String, Object> appRedisTemplate;
    /**
     * 更新店舖營業狀態
     * @param status
     * @return
     */
    @PutMapping("/{status}")
    @Operation(summary = "更新店舖營業狀態")
    public Result setStatus(@PathVariable Integer status){
        log.info("設置顛覆的營業狀態為：{}",status == 1 ? "營業中":"打烊中");
        appRedisTemplate.opsForValue().set(KEY,status);
        return Result.success();
    }

    /**
     * 取得店舖營業狀態
     * @return
     */
    @GetMapping("/status")
    @Operation(summary = "取得店舖營業狀態")
    public Result<Integer> getStatus(){
        Integer status = (Integer) appRedisTemplate.opsForValue().get(KEY);
        String statusText = status == null ? "未設置" : (status == 1 ? "營業中" : "打烊中");
        log.info("取得店舖的營業狀態為：{}", statusText);
        return Result.success(status);
    }


}
