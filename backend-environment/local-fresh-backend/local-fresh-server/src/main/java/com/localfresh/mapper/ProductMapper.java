package com.localfresh.mapper;

import com.github.pagehelper.Page;
import com.localfresh.annotation.AutoFill;
import com.localfresh.dto.ProductDTO;
import com.localfresh.dto.ProductPageQueryDTO;
import com.localfresh.entity.Product;
import com.localfresh.enumeration.OperationType;
import com.localfresh.vo.ProductVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProductMapper {

    /**
     * 根據分類id查詢商品數量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from product where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    /**
     * 插入商品資料
     * @param dish
     */
    @AutoFill(value = OperationType.INSERT)
    void insert(Product dish);

    /**
     * 商品的分頁查詢
     * @param dishPageQueryDTO
     * @return
     */
    Page<ProductVO> pageQuery(ProductPageQueryDTO dishPageQueryDTO);

    /**
     * 根據主鍵查詢商品
     * @param id
     * @return
     */
    @Select("select * from product where id = #{id}")
    Product getById(Long id);

    /**
     * 根據主鍵刪除商品資料
     * @param id
     */
    @Delete("delete from product where id = #{id}")
    void deleteById(Long id);

    /**
     * 根據商品id集合批次刪除商品
     * @param ids
     */
    void deleteByIds(List<Long> ids);

    /**
     * 根據id動態修改商品資料
     * @param dish
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(Product dish);

    /**
     * 动态條件查詢商品
     * @param dish
     * @return
     */
    List<Product> list(Product dish);


    /**
     * 根據直送箱id查詢商品
     * @param giftBoxId
     * @return
     */
    @Select("select a.* from product a left join gift_box_product b on a.id = b.product_id where b.gift_box_id = #{giftBoxId}")
    List<Product> getBySetmealId(Long giftBoxId);

    @Update("update product set stock = stock - #{quantity}, update_time = now() " +
            "where id = #{productId} and status = 1 and stock >= #{quantity}")
    int decreaseStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    @Update("update product set stock = stock + #{quantity}, update_time = now() where id = #{productId}")
    int increaseStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    @Update("update product set stock = stock + #{changeQuantity}, update_time = now() " +
            "where id = #{productId} and stock + #{changeQuantity} >= 0")
    int adjustStock(@Param("productId") Long productId, @Param("changeQuantity") Integer changeQuantity);

    /**
     * 查詢低庫存商品
     * @return 低庫存商品
     */
    List<ProductVO> listLowStock();

    /**
     * 統計低庫存商品數
     * @return 低庫存商品數
     */
    @Select("select count(id) from product where stock <= low_stock_threshold")
    Integer countLowStock();

    /**
     * 根據條件统计商品數量
     * @param map
     * @return
     */
    Integer countByMap(Map map);
}
