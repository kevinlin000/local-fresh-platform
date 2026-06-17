package com.localfresh.controller.admin;

import com.localfresh.dto.ProductDTO;
import com.localfresh.dto.ProductInventoryAdjustDTO;
import com.localfresh.dto.ProductPageQueryDTO;
import com.localfresh.entity.Product;
import com.localfresh.result.PageResult;
import com.localfresh.result.Result;
import com.localfresh.service.ProductService;
import com.localfresh.vo.ProductInventoryLogVO;
import com.localfresh.vo.ProductVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 商品管理
 */
@RestController
@RequestMapping("/admin/product")
@Tag(name = "商品相關介面")
@Slf4j
public class ProductController {

    @Autowired
    private ProductService productService;

    /**
     * 新增商品
     * @param dishDTO
     * @return
     */
    @PostMapping
    @Operation(summary = "新增商品")
    public Result save(@Valid @RequestBody ProductDTO dishDTO) {
        log.info("新增商品：{}", dishDTO);
        productService.saveWithFlavor(dishDTO);
        return Result.success();

    }

    /**
     * 商品分頁查詢
     * @param dishPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @Operation(summary = "商品分頁查詢")
    public Result<PageResult> page(ProductPageQueryDTO  dishPageQueryDTO) {
        log.info("商品分頁查詢：{}", dishPageQueryDTO);
        PageResult pageResult = productService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 商品的批次刪除
     * @param ids
     * @return
     */
    @DeleteMapping
    @Operation(summary = "批次刪除商品")
    public Result delete(@RequestParam List<Long> ids){
        log.info("商品批次刪除：{}", ids);
        productService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 根據 ID 查詢商品
     * @param id
     * @return
     */
    @GetMapping ("/{id}")
    @Operation(summary = "根據 ID 查詢商品")
    public Result<ProductVO> getById(@PathVariable Long id){
        log.info("根據 ID 查詢商品：{}", id);
        ProductVO dishVO = productService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    /**
     * 查詢商品庫存異動紀錄
     * @param id
     * @return
     */
    @GetMapping("/{id}/inventory-logs")
    @Operation(summary = "查詢商品庫存異動紀錄")
    public Result<List<ProductInventoryLogVO>> listInventoryLogs(@PathVariable Long id) {
        log.info("查詢商品庫存異動紀錄：{}", id);
        List<ProductInventoryLogVO> logs = productService.listInventoryLogs(id);
        return Result.success(logs);
    }

    /**
     * 手動調整商品庫存
     * @param id
     * @param productInventoryAdjustDTO
     * @return
     */
    @PatchMapping("/{id}/inventory")
    @Operation(summary = "手動調整商品庫存")
    public Result adjustInventory(@PathVariable Long id,
                                  @Valid @RequestBody ProductInventoryAdjustDTO productInventoryAdjustDTO) {
        log.info("手動調整商品庫存：id={}, data={}", id, productInventoryAdjustDTO);
        productService.adjustInventory(id, productInventoryAdjustDTO);
        return Result.success();
    }

    /**
     * 修改商品
     * @param dishDTO
     * @return
     */
    @PutMapping
    @Operation(summary = "修改商品")
    public Result update(@Valid @RequestBody ProductDTO dishDTO){
        log.info("修改商品：{}", dishDTO);
        productService.updateWithFlavor(dishDTO);
        return Result.success();

    }

    /**
     * 商品上架下架
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @Operation(summary = "商品上架下架")
    public Result<String> startOrStop(@PathVariable Integer status, Long id){
        productService.startOrStop(status, id);
        return Result.success();
    }

    /**
     * 根據分類 ID 查詢商品
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @Operation(summary = "根據分類 ID 查詢商品")
    public Result<List<Product>> list(Long categoryId){
        List<Product> list = productService.list(categoryId);
        return Result.success(list);
    }

}
