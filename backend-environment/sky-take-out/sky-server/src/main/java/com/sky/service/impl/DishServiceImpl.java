package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    /**
     * 新增菜品
     * @param dishDTO
     * @return
     */
    @Transactional
    public void saveWithFlavor(com.sky.dto.DishDTO dishDTO) {

        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        //向菜品表插入一條資料
        dishMapper.insert(dish);

        //獲取inset語句所生成的主按鍵值
        Long dishId = dish.getId();

        //向口味表插入n條資料
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors!=null&&flavors.size()>0){
            flavors.forEach(dishFlavor->{
                dishFlavor.setDishId(dishId);
            });
            //向口味表插入n條資料
            dishFlavorMapper.insertBatch(flavors);
        }


    }

    /**
     * 菜品分頁查詢
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO)  {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page= dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());

        }

    /**
     * 菜品的批量刪除
     * @param ids
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //判斷當前菜品是否能夠刪除 - 是否存在啟售中的菜品？
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if (dish.getStatus() == StatusConstant.ENABLE) {
                //當前菜品正在啟售中，無法刪除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        //判斷當前菜品是否能夠刪除 -是否被套餐關聯了？
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishId(ids);
        if(setmealIds!=null&&setmealIds.size()>0){
            // 當前菜品被套餐關聯了，不能刪除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }


        //刪除菜品表中的菜品資料
//        for (Long id : ids) {
//            dishMapper.deleteById(id);
//            //刪除菜品關聯的口味資料
//            dishFlavorMapper.deleteByDishId(id);
//        }

        //根據菜品id集合批量刪除菜品資料
        //sql: delete from dish where id in (?,?,?)
        dishMapper.deleteByIds(ids);
        //根據菜品id集合批量刪除關聯的口味資料
        //sql: delete from dish_flavor where id in (?,?,?)
        dishFlavorMapper.deleteByDishIds(ids);


    }

    /**
     *  根據id查詢菜品和對應的口味
     * @param id
     * @return
     */

    public DishVO getByIdWithFlavor(Long id) {
        //根據id查詢菜品資料
        Dish dish = dishMapper.getById(id);
        //根據菜品id查詢口味資料
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);

        //講查詢到的資料封裝到DishVO
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavors);

        return dishVO;
    }

    /**
     * 根據id修改菜品基本資訊和對應的口味資訊
     * @param dishDTO
     */

    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        //修改菜品表基本資訊
        dishMapper.update(dish);

        //刪除原有的口味資訊
        dishFlavorMapper.deleteByDishId(dishDTO.getId());

        //重新插入口味資訊
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors!=null && flavors.size()>0){
            flavors.forEach(dishFlavor->{
                dishFlavor.setDishId(dishDTO.getId());
            });
            //向口味表插入n條資料
            dishFlavorMapper.insertBatch(flavors);
        }


    }
}
