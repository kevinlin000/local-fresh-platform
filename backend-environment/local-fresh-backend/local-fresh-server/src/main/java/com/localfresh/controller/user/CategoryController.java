package com.localfresh.controller.user;

import com.localfresh.entity.Category;
import com.localfresh.result.Result;
import com.localfresh.service.CategoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController("userCategoryController")
@RequestMapping("/user/category")
@Tag(name = "會員端-分類介面")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 查詢分類
     * @param type
     * @return
     */
    @GetMapping("/list")
    @Operation(summary = "查詢分類")
    public Result<List<Category>> list(Integer type) {
        List<Category> list = categoryService.list(type);
        return Result.success(list);
    }
}
