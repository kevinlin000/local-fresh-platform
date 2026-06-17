package com.localfresh.mapper;

import com.github.pagehelper.Page;
import com.localfresh.annotation.AutoFill;
import com.localfresh.dto.GiftBoxPageQueryDTO;
import com.localfresh.entity.GiftBox;
import com.localfresh.enumeration.OperationType;
import com.localfresh.vo.ProductItemVO;
import com.localfresh.vo.GiftBoxVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface GiftBoxMapper {

    /**
     * 根據分類id查詢直送箱的數量
     * @param id
     * @return
     */
    @Select("select count(id) from gift_box where category_id = #{categoryId}")
    Integer countByCategoryId(Long id);

    /**
     * 新增直送箱
     * @param setmeal
     */
    @AutoFill(OperationType.INSERT)
    void insert(GiftBox setmeal);

    /**
     * 分頁查詢
     * @param setmealPageQueryDTO
     * @return
     */
    Page<GiftBoxVO> pageQuery(GiftBoxPageQueryDTO setmealPageQueryDTO);

    /**
     * 根據id查詢直送箱
     * @param id
     * @return
     */
    @Select("select * from gift_box where id = #{id}")
    GiftBox getById(Long id);

    /**
     * 根據id刪除直送箱
     * @param giftBoxId
     */
    @Delete("delete from gift_box where id = #{id}")
    void deleteById(Long giftBoxId);

    /**
     * 動態條件查詢直送箱
     * @param setmeal
     * @return
     */
    List<GiftBox> list(GiftBox setmeal);

    /**
     * 根據直送箱id查詢商品選項
     * @param giftBoxId
     * @return
     */
    @Select("select gbp.name, gbp.copies, p.image, p.description " +
            "from gift_box_product gbp left join product p on gbp.product_id = p.id " +
            "where gbp.gift_box_id = #{giftBoxId}")
    List<ProductItemVO> getDishItemBySetmealId(Long giftBoxId);

    /**
     * 根據id修改直送箱
     *
     * @param setmeal
     */
    @AutoFill(OperationType.UPDATE)
    void update(GiftBox setmeal);

    /**
     * 根據條件統計直送箱數量
     * @param map
     * @return
     */
    Integer countByMap(Map map);
}
