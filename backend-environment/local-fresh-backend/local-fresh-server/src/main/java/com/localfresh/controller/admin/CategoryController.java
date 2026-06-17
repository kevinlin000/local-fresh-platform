package com.localfresh.controller.admin;

import com.localfresh.dto.CategoryDTO;
import com.localfresh.dto.CategoryPageQueryDTO;
import com.localfresh.entity.Category;
import com.localfresh.result.PageResult;
import com.localfresh.result.Result;
import com.localfresh.service.CategoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 分類管理
 */
@RestController
@RequestMapping("/admin/category")
@Tag(name = "分類相關介面")
@Slf4j
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 新增分類
     * @param categoryDTO
     * @return
     */
    @PostMapping
    @Operation(summary = "新增分類")
    public Result<String> save(@Valid @RequestBody CategoryDTO categoryDTO){
        log.info("新增分類：{}", categoryDTO);
        categoryService.save(categoryDTO);
        return Result.success();
    }

    /**
     * 分類分頁查詢
     * @param categoryPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @Operation(summary = "分類分頁查詢")
    public Result<PageResult> page(CategoryPageQueryDTO categoryPageQueryDTO){
        log.info("分頁查詢：{}", categoryPageQueryDTO);
        PageResult pageResult = categoryService.pageQuery(categoryPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 刪除分類
     * @param id
     * @return
     */
    @DeleteMapping
    @Operation(summary = "刪除分類")
    public Result<String> deleteById(Long id){
        log.info("刪除分類：{}", id);
        categoryService.deleteById(id);
        return Result.success();
    }

    /**
     * 修改分類
     * @param categoryDTO
     * @return
     */
    @PutMapping
    @Operation(summary = "修改分類")
    public Result<String> update(@Valid @RequestBody CategoryDTO categoryDTO){
        categoryService.update(categoryDTO);
        return Result.success();
    }

    /**
     * 啟用、停用分類
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @Operation(summary = "啟用停用分類")
    public Result<String> startOrStop(@PathVariable("status") Integer status, Long id){
        categoryService.startOrStop(status,id);
        return Result.success();
    }

    /**
     * 根據類型查詢分類
     * @param type
     * @return
     */
    @GetMapping("/list")
    @Operation(summary = "根據類型查詢分類")
    public Result<List<Category>> list(Integer type){
        List<Category> list = categoryService.list(type);
        return Result.success(list);
    }
}
