package com.localfresh.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.localfresh.constant.MessageConstant;
import com.localfresh.constant.StatusConstant;
import com.localfresh.context.BaseContext;
import com.localfresh.dto.CategoryDTO;
import com.localfresh.dto.CategoryPageQueryDTO;
import com.localfresh.entity.Category;
import com.localfresh.exception.DeletionNotAllowedException;
import com.localfresh.mapper.CategoryMapper;
import com.localfresh.mapper.ProductMapper;
import com.localfresh.mapper.GiftBoxMapper;
import com.localfresh.result.PageResult;
import com.localfresh.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 分類業務层
 */
@Service
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private GiftBoxMapper giftBoxMapper;

    /**
     * 新增分類
     * @param categoryDTO
     */
    public void save(CategoryDTO categoryDTO) {
        Category category = new Category();
        //屬性拷貝
        BeanUtils.copyProperties(categoryDTO, category);

        //分類狀態預設為停用狀態 0
        category.setStatus(StatusConstant.DISABLE);

        categoryMapper.insert(category);
    }

    /**
     * 分頁查詢
     * @param categoryPageQueryDTO
     * @return
     */
    public PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO) {
        PageHelper.startPage(categoryPageQueryDTO.getPage(),categoryPageQueryDTO.getPageSize());
        //下一条sql进行分頁，自动加入limit关键字分頁
        Page<Category> page = categoryMapper.pageQuery(categoryPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 根據id刪除分類
     * @param id
     */
    public void deleteById(Long id) {
        //查詢目前分類是否關聯了商品，如果關聯了就拋出業務例外
        Integer count = productMapper.countByCategoryId(id);
        if(count > 0){
            //目前分類下有商品，不能刪除
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_DISH);
        }

        //查詢目前分類是否關聯了直送箱，如果關聯了就拋出業務例外
        count = giftBoxMapper.countByCategoryId(id);
        if(count > 0){
            //目前分類下有商品，不能刪除
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL);
        }

        //刪除分類資料
        categoryMapper.deleteById(id);
    }

    /**
     * 修改分類
     * @param categoryDTO
     */
    public void update(CategoryDTO categoryDTO) {
        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO, category);

        categoryMapper.update(category);
    }

    /**
     * 啟用、停用分類
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id) {
        Category category = Category.builder()
                .id(id)
                .status(status)
                //.updateTime(LocalDateTime.now())
                //.updateUser(BaseContext.getCurrentId())
                .build();
        categoryMapper.update(category);
    }

    /**
     * 根據類型查詢分類
     * @param type
     * @return
     */
    public List<Category> list(Integer type) {
        return categoryMapper.list(type);
    }
}
