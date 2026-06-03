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

import javax.validation.Valid;
import java.util.List;

/**
 * 單品管理
 */
@RestController
@RequestMapping("/admin/product")
@Api(tags = "單品相關介面")
@Slf4j
public class ProductController {

    @Autowired
    private ProductService productService;

    /**
     * 新增單品
     * @param dishDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增單品")
    public Result save(@Valid @RequestBody ProductDTO dishDTO) {
        log.info("新增單品：{}", dishDTO);
        productService.saveWithFlavor(dishDTO);
        return Result.success();

    }

    /**
     * 單品分頁查詢
     * @param dishPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("單品分頁查詢")
    public Result<PageResult> page(ProductPageQueryDTO  dishPageQueryDTO) {
        log.info("單品分頁查詢：{}", dishPageQueryDTO);
        PageResult pageResult = productService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 單品的批量刪除
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("批量刪除單品")
    public Result delete(@RequestParam List<Long> ids){
        log.info("單品批量刪除：{}", ids);
        productService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 根據 ID 查詢單品
     * @param id
     * @return
     */
    @GetMapping ("/{id}")
    @ApiOperation("根據 ID 查詢單品")
    public Result<ProductVO> getById(@PathVariable Long id){
        log.info("根據 ID 查詢單品：{}", id);
        ProductVO dishVO = productService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    /**
     * 修改單品
     * @param dishDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改單品")
    public Result update(@Valid @RequestBody ProductDTO dishDTO){
        log.info("修改單品：{}", dishDTO);
        productService.updateWithFlavor(dishDTO);
        return Result.success();

    }

    /**
     * 單品起售停售
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("單品上架下架")
    public Result<String> startOrStop(@PathVariable Integer status, Long id){
        productService.startOrStop(status, id);
        return Result.success();
    }

    /**
     * 根據分類 ID 查詢單品
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根據分類 ID 查詢單品")
    public Result<List<Product>> list(Long categoryId){
        List<Product> list = productService.list(categoryId);
        return Result.success(list);
    }

}
