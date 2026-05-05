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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    /**
     * 新增直送箱
     * @param setmealDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增直送箱")
    @CacheEvict(cacheNames = "setmealCache",key = "#setmealDTO.categoryId") //key: setmealCache::100
    public Result save(@RequestBody GiftBoxDTO setmealDTO) {
        giftBoxService.saveWithDish(setmealDTO);
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
    @CacheEvict(cacheNames = "setmealCache",allEntries = true) //清除setmealCache缓存中所有数据
    public Result delete(@RequestParam List<Long> ids){
        giftBoxService.deleteBatch(ids);
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
    @CacheEvict(cacheNames = "setmealCache",allEntries = true)
    public Result update(@RequestBody GiftBoxDTO setmealDTO) {
        giftBoxService.update(setmealDTO);
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
    @CacheEvict(cacheNames = "setmealCache",allEntries = true)
    public Result startOrStop(@PathVariable Integer status, Long id) {
        giftBoxService.startOrStop(status, id);
        return Result.success();
    }

}
