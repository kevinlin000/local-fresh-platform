package com.localfresh.controller.admin;

import com.localfresh.result.Result;
import com.localfresh.exception.BaseException;
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
    private static final Integer DEFAULT_STATUS = 1;

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
        if (status == null || (status != 0 && status != 1)) {
            throw new BaseException("店舖狀態只能是 0 或 1");
        }
        log.info("設置店舖的營業狀態為：{}",status == 1 ? "營業中":"打烊中");
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
        if (status == null) {
            status = DEFAULT_STATUS;
            appRedisTemplate.opsForValue().set(KEY, status);
        }
        String statusText = status == 1 ? "營業中" : "打烊中";
        log.info("取得店舖的營業狀態為：{}", statusText);
        return Result.success(status);
    }


}
