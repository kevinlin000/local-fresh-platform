package com.localfresh.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.localfresh.constant.MessageConstant;
import com.localfresh.constant.StatusConstant;
import com.localfresh.dto.GiftBoxDTO;
import com.localfresh.dto.GiftBoxPageQueryDTO;
import com.localfresh.entity.Product;
import com.localfresh.entity.GiftBox;
import com.localfresh.entity.GiftBoxProduct;
import com.localfresh.exception.DeletionNotAllowedException;
import com.localfresh.exception.SetmealEnableFailedException;
import com.localfresh.mapper.ProductMapper;
import com.localfresh.mapper.GiftBoxProductMapper;
import com.localfresh.mapper.GiftBoxMapper;
import com.localfresh.result.PageResult;
import com.localfresh.service.GiftBoxService;
import com.localfresh.vo.ProductItemVO;
import com.localfresh.vo.GiftBoxVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 套餐业务实现
 */
@Service
@Slf4j
public class GiftBoxServiceImpl implements GiftBoxService {

    @Autowired
    private GiftBoxMapper giftBoxMapper;
    @Autowired
    private GiftBoxProductMapper giftBoxProductMapper;
    @Autowired
    private ProductMapper productMapper;

    /**
     * 新增套餐，同时需要保存套餐和菜品的关联关系
     * @param setmealDTO
     */
    @Transactional
    public void saveWithDish(GiftBoxDTO setmealDTO) {
        GiftBox setmeal = new GiftBox();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        //向套餐表插入数据
        giftBoxMapper.insert(setmeal);

        //获取生成的套餐id
        Long giftBoxId = setmeal.getId();

        List<GiftBoxProduct> giftBoxProducts = setmealDTO.getGiftBoxProducts();
        giftBoxProducts.forEach(setmealDish -> {
            setmealDish.setGiftBoxId(giftBoxId);
        });

        //保存套餐和菜品的关联关系
        giftBoxProductMapper.insertBatch(giftBoxProducts);
    }

    /**
     * 分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    public PageResult pageQuery(GiftBoxPageQueryDTO setmealPageQueryDTO) {
        int pageNum = setmealPageQueryDTO.getPage();
        int pageSize = setmealPageQueryDTO.getPageSize();

        PageHelper.startPage(pageNum, pageSize);
        Page<GiftBoxVO> page = giftBoxMapper.pageQuery(setmealPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 批量删除套餐
     * @param ids
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {
        ids.forEach(id -> {
            GiftBox setmeal = giftBoxMapper.getById(id);
            if(StatusConstant.ENABLE == setmeal.getStatus()){
                //起售中的套餐不能删除
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        });

        ids.forEach(giftBoxId -> {
            //删除套餐表中的数据
            giftBoxMapper.deleteById(giftBoxId);
            //删除套餐菜品关系表中的数据
            giftBoxProductMapper.deleteBySetmealId(giftBoxId);
        });
    }

    /**
     * 根据id查询套餐和套餐菜品关系
     *
     * @param id
     * @return
     */
    public GiftBoxVO getByIdWithDish(Long id) {
        GiftBox setmeal = giftBoxMapper.getById(id);
        List<GiftBoxProduct> giftBoxProducts = giftBoxProductMapper.getBySetmealId(id);

        GiftBoxVO setmealVO = new GiftBoxVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setGiftBoxProducts(giftBoxProducts);

        return setmealVO;
    }

    /**
     * 修改套餐
     *
     * @param setmealDTO
     */
    @Transactional
    public void update(GiftBoxDTO setmealDTO) {
        GiftBox setmeal = new GiftBox();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        //1、修改套餐表，执行update
        giftBoxMapper.update(setmeal);

        //套餐id
        Long giftBoxId = setmealDTO.getId();

        //2、删除套餐和菜品的关联关系，操作setmeal_dish表，执行delete
        giftBoxProductMapper.deleteBySetmealId(giftBoxId);

        List<GiftBoxProduct> giftBoxProducts = setmealDTO.getGiftBoxProducts();
        giftBoxProducts.forEach(setmealDish -> {
            setmealDish.setGiftBoxId(giftBoxId);
        });
        //3、重新插入套餐和菜品的关联关系，操作setmeal_dish表，执行insert
        giftBoxProductMapper.insertBatch(giftBoxProducts);
    }

    /**
     * 套餐起售、停售
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id) {
        //起售套餐时，判断套餐内是否有停售菜品，有停售菜品提示"套餐内包含未启售菜品，无法启售"
        if(status == StatusConstant.ENABLE){
            // select a.* from product a left join gift_box_product b on a.id = b.product_id where b.gift_box_id = ?
            List<Product> dishList = productMapper.getBySetmealId(id);
            if(dishList != null && dishList.size() > 0){
                dishList.forEach(dish -> {
                    if(StatusConstant.DISABLE == dish.getStatus()){
                        throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                });
            }
        }

        GiftBox setmeal = GiftBox.builder()
                .id(id)
                .status(status)
                .build();
        giftBoxMapper.update(setmeal);
    }

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<GiftBox> list(GiftBox setmeal) {
        List<GiftBox> list = giftBoxMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<ProductItemVO> getDishItemById(Long id) {
        return giftBoxMapper.getDishItemBySetmealId(id);
    }

}
