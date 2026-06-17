package com.localfresh.mapper;

import com.localfresh.entity.ProductSpec;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductSpecMapper {
    /**
     * 批次插入規格資料
     * @param productSpecs
     */
    void insertBatch(List<ProductSpec> productSpecs);

    /**
     * 根據商品id刪除對應的規格資料
     * @param productId
     */
    @Delete("delete from product_spec where product_id = #{productId}")
    void deleteByDishId(Long productId);

    /**
     * 根據商品id集合批次刪除對應的規格資料
     * @param dishIds
     */
    void deleteByDishIds(List<Long> dishIds);

    /**
     * 根據商品id查詢對應的規格資料
     * @param productId
     * @return
     */
    @Select("select * from product_spec where product_id = #{productId}")
    List<ProductSpec> getByDishId(Long productId);
}
