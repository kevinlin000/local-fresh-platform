package com.sky.service;

import com.sky.dto.ProductDTO;
import com.sky.dto.ProductPageQueryDTO;
import com.sky.entity.Product;
import com.sky.result.PageResult;
import com.sky.vo.ProductInventoryLogVO;
import com.sky.vo.ProductVO;

import java.util.List;

public interface ProductService {

    /**
     * 新增菜品和對應的口味
     * @param dishDTO
     */
    public void saveWithFlavor(ProductDTO dishDTO);

    /**
     * 菜品分頁查詢
     * @param dishPageQueryDTO
     * @return
     */
    PageResult pageQuery(ProductPageQueryDTO dishPageQueryDTO);

    /**
     * 菜品的批量刪除
     * @param ids
     */
    void deleteBatch(List<Long> ids);

    /**
     *  根據id查詢菜品和對應的口味
     * @param id
     * @return
     */
    ProductVO getByIdWithFlavor(Long id);

    /**
     * 根據id修改菜品基本資訊和對應的口味資訊
     * @param dishDTO
     */
    void updateWithFlavor(ProductDTO dishDTO);

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    List<Product> list(Long categoryId);

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    List<ProductVO> listWithFlavor(Product dish);

    /**
     * 菜品起售停售
     * @param status
     * @param id
     */
    void startOrStop(Integer status, Long id);

    /**
     * 查詢商品庫存異動紀錄
     * @param id 商品 ID
     * @return 庫存異動紀錄
     */
    List<ProductInventoryLogVO> listInventoryLogs(Long id);
}
