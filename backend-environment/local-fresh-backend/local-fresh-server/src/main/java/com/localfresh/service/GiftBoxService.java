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
     * 新增直送箱，同時需要保存直送箱和商品的關聯關係
     * @param setmealDTO
     */
    void saveWithDish(GiftBoxDTO setmealDTO);

    /**
     * 分頁查詢
     * @param setmealPageQueryDTO
     * @return
     */
    PageResult pageQuery(GiftBoxPageQueryDTO setmealPageQueryDTO);

    /**
     * 批次刪除直送箱
     * @param ids
     */
    void deleteBatch(List<Long> ids);

    /**
     * 根據id查詢直送箱和關聯的商品資料
     * @param id
     * @return
     */
    GiftBoxVO getByIdWithDish(Long id);

    /**
     * 修改直送箱
     * @param setmealDTO
     */
    void update(GiftBoxDTO setmealDTO);

    /**
     * 直送箱起售、停售
     * @param status
     * @param id
     */
    void startOrStop(Integer status, Long id);

    /**
     * 條件查詢
     * @param setmeal
     * @return
     */
    List<GiftBox> list(GiftBox setmeal);

    /**
     * 根據id查詢商品选项
     * @param id
     * @return
     */
    List<ProductItemVO> getDishItemById(Long id);
}
