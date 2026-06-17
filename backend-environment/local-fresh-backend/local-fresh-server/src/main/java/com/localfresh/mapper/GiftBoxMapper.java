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
     * 根据分类id查询套餐的数量
     * @param id
     * @return
     */
    @Select("select count(id) from gift_box where category_id = #{categoryId}")
    Integer countByCategoryId(Long id);

    /**
     * 新增套餐
     * @param setmeal
     */
    @AutoFill(OperationType.INSERT)
    void insert(GiftBox setmeal);

    /**
     * 分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    Page<GiftBoxVO> pageQuery(GiftBoxPageQueryDTO setmealPageQueryDTO);

    /**
     * 根据id查询套餐
     * @param id
     * @return
     */
    @Select("select * from gift_box where id = #{id}")
    GiftBox getById(Long id);

    /**
     * 根据id删除套餐
     * @param giftBoxId
     */
    @Delete("delete from gift_box where id = #{id}")
    void deleteById(Long giftBoxId);

    /**
     * 动态条件查询套餐
     * @param setmeal
     * @return
     */
    List<GiftBox> list(GiftBox setmeal);

    /**
     * 根据套餐id查询菜品选项
     * @param giftBoxId
     * @return
     */
    @Select("select gbp.name, gbp.copies, p.image, p.description " +
            "from gift_box_product gbp left join product p on gbp.product_id = p.id " +
            "where gbp.gift_box_id = #{giftBoxId}")
    List<ProductItemVO> getDishItemBySetmealId(Long giftBoxId);

    /**
     * 根据id修改套餐
     *
     * @param setmeal
     */
    @AutoFill(OperationType.UPDATE)
    void update(GiftBox setmeal);

    /**
     * 根据条件统计套餐数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);
}
