package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.ProductDTO;
import com.sky.dto.ProductPageQueryDTO;
import com.sky.entity.Product;
import com.sky.enumeration.OperationType;
import com.sky.vo.ProductVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProductMapper {

    /**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from product where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    /**
     * 插入菜品資料
     * @param dish
     */
    @AutoFill(value = OperationType.INSERT)
    void insert(Product dish);

    /**
     * 菜品的分頁查詢
     * @param dishPageQueryDTO
     * @return
     */
    Page<ProductVO> pageQuery(ProductPageQueryDTO dishPageQueryDTO);

    /**
     * 根據主鍵查詢菜品
     * @param id
     * @return
     */
    @Select("select * from product where id = #{id}")
    Product getById(Long id);

    /**
     * 根據主鍵刪除菜品資料
     * @param id
     */
    @Delete("delete from product where id = #{id}")
    void deleteById(Long id);

    /**
     * 根據菜品id集合批量刪除菜品
     * @param ids
     */
    void deleteByIds(List<Long> ids);

    /**
     * 根據id動態修改菜品資料
     * @param dish
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(Product dish);

    /**
     * 动态条件查询菜品
     * @param dish
     * @return
     */
    List<Product> list(Product dish);


    /**
     * 根据套餐id查询菜品
     * @param giftBoxId
     * @return
     */
    @Select("select a.* from product a left join gift_box_product b on a.id = b.product_id where b.gift_box_id = #{giftBoxId}")
    List<Product> getBySetmealId(Long giftBoxId);

    /**
     * 根据条件统计菜品数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);
}
