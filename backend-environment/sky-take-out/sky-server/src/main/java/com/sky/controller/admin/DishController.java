package com.sky.controller.admin;

import com.sky.dto.ProductDTO;
import com.sky.dto.ProductPageQueryDTO;
import com.sky.entity.Product;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.ProductService;
import com.sky.vo.ProductVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜品管理
 */
@RestController
@RequestMapping("/admin/dish")
@Api(tags = "菜品相關接口")
@Slf4j
public class DishController {

    @Autowired
    private ProductService productService;

    /**
     * 新增菜品
     * @param dishDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody ProductDTO dishDTO) {
        log.info("新增菜品：{}", dishDTO);
        productService.saveWithFlavor(dishDTO);
        return Result.success();

    }

    /**
     * 菜品分頁查詢
     * @param dishPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("菜品分頁查詢")
    public Result<PageResult> page(ProductPageQueryDTO  dishPageQueryDTO) {
        log.info("菜品分頁查詢：{}", dishPageQueryDTO);
        PageResult pageResult = productService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 菜品的批量刪除
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("批量刪除菜品")
    public Result delete(@RequestParam List<Long> ids){
        log.info("菜品批量刪除：{}", ids);
        productService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 根據id查詢菜品
     * @param id
     * @return
     */
    @GetMapping ("/{id}")
    @ApiOperation("根據id查詢菜品")
    public Result<ProductVO> getById(@PathVariable Long id){
        log.info("根據id查詢菜品：{}", id);
        ProductVO dishVO = productService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    /**
     * 修改菜品
     * @param dishDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改菜品")
    public Result update(@RequestBody ProductDTO dishDTO){
        log.info("修改菜品：{}", dishDTO);
        productService.updateWithFlavor(dishDTO);
        return Result.success();

    }

    /**
     * 菜品起售停售
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("菜品起售停售")
    public Result<String> startOrStop(@PathVariable Integer status, Long id){
        productService.startOrStop(status, id);
        return Result.success();
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<Product>> list(Long categoryId){
        List<Product> list = productService.list(categoryId);
        return Result.success(list);
    }

}
