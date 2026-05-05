package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.ProductDTO;
import com.sky.dto.ProductPageQueryDTO;
import com.sky.entity.Product;
import com.sky.entity.ProductSpec;
import com.sky.entity.GiftBox;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.ProductVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 新增菜品
     * @param dishDTO
     * @return
     */
    @Transactional
    public void saveWithFlavor(com.sky.dto.ProductDTO dishDTO) {

        Product dish = new Product();
        BeanUtils.copyProperties(dishDTO, dish);

        //向菜品表插入一條資料
        dishMapper.insert(dish);

        //獲取inset語句所生成的主按鍵值
        Long productId = dish.getId();

        //向口味表插入n條資料
        List<ProductSpec> productSpecs = dishDTO.getProductSpecs();
        if(productSpecs!=null&&productSpecs.size()>0){
            productSpecs.forEach(productSpec->{
                productSpec.setProductId(productId);
            });
            //向口味表插入n條資料
            dishFlavorMapper.insertBatch(productSpecs);
        }

        cleanCache("dish_" + dishDTO.getCategoryId());
    }

    /**
     * 菜品分頁查詢
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(ProductPageQueryDTO dishPageQueryDTO)  {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<ProductVO> page= dishMapper.pageQuery(dishPageQueryDTO);
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
            Product dish = dishMapper.getById(id);
            if (dish.getStatus() == StatusConstant.ENABLE) {
                //當前菜品正在啟售中，無法刪除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        //判斷當前菜品是否能夠刪除 -是否被套餐關聯了？
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if(setmealIds!=null&&setmealIds.size()>0){
            // 當前菜品被套餐關聯了，不能刪除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }


        //根據菜品id集合批量刪除菜品資料
        //sql: delete from dish where id in (?,?,?)
        dishMapper.deleteByIds(ids);
        //根據菜品id集合批量刪除關聯的口味資料
        //sql: delete from dish_flavor where id in (?,?,?)
        dishFlavorMapper.deleteByDishIds(ids);

        cleanCache("dish_*");
    }

    /**
     *  根據id查詢菜品和對應的口味
     * @param id
     * @return
     */

    public ProductVO getByIdWithFlavor(Long id) {
        //根據id查詢菜品資料
        Product dish = dishMapper.getById(id);
        //根據菜品id查詢口味資料
        List<ProductSpec> dishFlavors = dishFlavorMapper.getByDishId(id);

        //講查詢到的資料封裝到ProductVO
        ProductVO dishVO = new ProductVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setProductSpecs(dishFlavors);

        return dishVO;
    }

    /**
     * 根據id修改菜品基本資訊和對應的口味資訊
     * @param dishDTO
     */

    public void updateWithFlavor(ProductDTO dishDTO) {
        Product dish = new Product();
        BeanUtils.copyProperties(dishDTO, dish);
        //修改菜品表基本資訊
        dishMapper.update(dish);

        //刪除原有的口味資訊
        dishFlavorMapper.deleteByDishId(dishDTO.getId());

        //重新插入口味資訊
        List<ProductSpec> productSpecs = dishDTO.getProductSpecs();
        if(productSpecs!=null && productSpecs.size()>0){
            productSpecs.forEach(productSpec->{
                productSpec.setProductId(dishDTO.getId());
            });
            //向口味表插入n條資料
            dishFlavorMapper.insertBatch(productSpecs);
        }

        cleanCache("dish_*");
    }


    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    public List<Product> list(Long categoryId) {
        Product dish = Product.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        return dishMapper.list(dish);
    }

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<ProductVO> listWithFlavor(Product dish) {
        List<Product> dishList = dishMapper.list(dish);

        List<ProductVO> dishVOList = new ArrayList<>();

        for (Product d : dishList) {
            ProductVO dishVO = new ProductVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<ProductSpec> productSpecs = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setProductSpecs(productSpecs);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }

    /**
     * 菜品起售停售
     *
     * @param status
     * @param id
     */
    @Transactional
    public void startOrStop(Integer status, Long id) {
        Product dish = Product.builder()
                .id(id)
                .status(status)
                .build();
        dishMapper.update(dish);

        if (status == StatusConstant.DISABLE) {
            // 如果是停售操作，还需要将包含当前菜品的套餐也停售
            List<Long> dishIds = new ArrayList<>();
            dishIds.add(id);
            // select setmeal_id from setmeal_dish where dish_id in (?,?,?)
            List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(dishIds);
            if (setmealIds != null && setmealIds.size() > 0) {
                for (Long giftBoxId : setmealIds) {
                    GiftBox setmeal = GiftBox.builder()
                            .id(giftBoxId)
                            .status(StatusConstant.DISABLE)
                            .build();
                    setmealMapper.update(setmeal);
                }
            }
        }

        cleanCache("dish_*");
    }

    private void cleanCache(String pattern) {
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }

}
