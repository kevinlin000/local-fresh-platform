package com.localfresh.service;

import com.localfresh.dto.GiftBoxDTO;
import com.localfresh.dto.GiftBoxPageQueryDTO;
import com.localfresh.entity.GiftBox;
import com.localfresh.result.PageResult;
import com.localfresh.vo.ProductItemVO;
import com.localfresh.vo.GiftBoxVO;

import java.util.List;

public interface GiftBoxService {

    /**
     * 新增套餐，同时需要保存套餐和菜品的关联关系
     * @param setmealDTO
     */
    void saveWithDish(GiftBoxDTO setmealDTO);

    /**
     * 分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    PageResult pageQuery(GiftBoxPageQueryDTO setmealPageQueryDTO);

    /**
     * 批量删除套餐
     * @param ids
     */
    void deleteBatch(List<Long> ids);

    /**
     * 根据id查询套餐和关联的菜品数据
     * @param id
     * @return
     */
    GiftBoxVO getByIdWithDish(Long id);

    /**
     * 修改套餐
     * @param setmealDTO
     */
    void update(GiftBoxDTO setmealDTO);

    /**
     * 套餐起售、停售
     * @param status
     * @param id
     */
    void startOrStop(Integer status, Long id);

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    List<GiftBox> list(GiftBox setmeal);

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    List<ProductItemVO> getDishItemById(Long id);
}
