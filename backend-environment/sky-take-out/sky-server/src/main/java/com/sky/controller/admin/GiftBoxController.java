package com.sky.controller.admin;

import com.sky.dto.GiftBoxDTO;
import com.sky.dto.GiftBoxPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.GiftBoxService;
import com.sky.vo.GiftBoxVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;

/**
 * 直送箱管理
 */
@RestController
@RequestMapping("/admin/giftbox")
@Api(tags = "直送箱相關接口")
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
    @ApiOperation("新增直送箱")
    public Result save(@RequestBody GiftBoxDTO setmealDTO) {
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
    @ApiOperation("直送箱分頁查詢")
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
    @ApiOperation("批量刪除直送箱")
    public Result delete(@RequestParam List<Long> ids){
        giftBoxService.deleteBatch(ids);
        cleanCache("giftbox_*");
        return Result.success();
    }

    /**
     * 根据id查询直送箱，用于修改页面回显数据
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询直送箱")
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
    @ApiOperation("修改直送箱")
    public Result update(@RequestBody GiftBoxDTO setmealDTO) {
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
    @ApiOperation("直送箱上架下架")
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
