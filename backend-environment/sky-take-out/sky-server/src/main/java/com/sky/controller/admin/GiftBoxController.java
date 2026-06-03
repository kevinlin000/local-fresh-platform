package com.sky.controller.admin;

import com.sky.dto.GiftBoxDTO;
import com.sky.dto.GiftBoxPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.GiftBoxService;
import com.sky.vo.GiftBoxVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;

/**
 * 直送箱管理
 */
@RestController
@RequestMapping("/admin/giftbox")
@Tag(name = "直送箱相關介面")
@Slf4j
public class GiftBoxController {

    @Autowired
    private GiftBoxService giftBoxService;

    @Resource(name = "appRedisTemplate")
    private RedisTemplate<String, Object> appRedisTemplate;

    /**
     * 新增直送箱
     * @param setmealDTO
     * @return
     */
    @PostMapping
    @Operation(summary = "新增直送箱")
    public Result save(@Valid @RequestBody GiftBoxDTO setmealDTO) {
        giftBoxService.saveWithDish(setmealDTO);
        cleanCache("giftbox_*");
        return Result.success();
    }

    /**
     * 直送箱分頁查詢
     * @param setmealPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @Operation(summary = "直送箱分頁查詢")
    public Result<PageResult> page(GiftBoxPageQueryDTO setmealPageQueryDTO) {
        PageResult pageResult = giftBoxService.pageQuery(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 批量刪除直送箱
     * @param ids
     * @return
     */
    @DeleteMapping
    @Operation(summary = "批量刪除直送箱")
    public Result delete(@RequestParam List<Long> ids){
        giftBoxService.deleteBatch(ids);
        cleanCache("giftbox_*");
        return Result.success();
    }

    /**
     * 根據 ID 查詢直送箱，用于修改页面回显資料
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @Operation(summary = "根據 ID 查詢直送箱")
    public Result<GiftBoxVO> getById(@PathVariable Long id) {
        GiftBoxVO setmealVO = giftBoxService.getByIdWithDish(id);
        return Result.success(setmealVO);
    }

    /**
     * 修改直送箱
     *
     * @param setmealDTO
     * @return
     */
    @PutMapping
    @Operation(summary = "修改直送箱")
    public Result update(@Valid @RequestBody GiftBoxDTO setmealDTO) {
        giftBoxService.update(setmealDTO);
        cleanCache("giftbox_*");
        return Result.success();
    }

    /**
     * 直送箱起售停售
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @Operation(summary = "直送箱上架下架")
    public Result startOrStop(@PathVariable Integer status, Long id) {
        giftBoxService.startOrStop(status, id);
        cleanCache("giftbox_*");
        return Result.success();
    }

    private void cleanCache(String pattern) {
        Set<String> keys = appRedisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            appRedisTemplate.delete(keys);
        }
    }

}
