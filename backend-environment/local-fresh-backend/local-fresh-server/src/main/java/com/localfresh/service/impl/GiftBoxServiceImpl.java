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
 * 直送箱業務实现
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
     * 新增直送箱，同時需要保存直送箱和商品的關聯關係
     * @param setmealDTO
     */
    @Transactional
    public void saveWithDish(GiftBoxDTO setmealDTO) {
        GiftBox setmeal = new GiftBox();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        //向直送箱表插入資料
        giftBoxMapper.insert(setmeal);

        //取得產生的直送箱id
        Long giftBoxId = setmeal.getId();

        List<GiftBoxProduct> giftBoxProducts = setmealDTO.getGiftBoxProducts();
        giftBoxProducts.forEach(setmealDish -> {
            setmealDish.setGiftBoxId(giftBoxId);
        });

        //保存直送箱和商品的關聯關係
        giftBoxProductMapper.insertBatch(giftBoxProducts);
    }

    /**
     * 分頁查詢
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
     * 批次刪除直送箱
     * @param ids
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {
        ids.forEach(id -> {
            GiftBox setmeal = giftBoxMapper.getById(id);
            if(StatusConstant.ENABLE == setmeal.getStatus()){
                //起售中的直送箱不能刪除
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        });

        ids.forEach(giftBoxId -> {
            //刪除直送箱表中的資料
            giftBoxMapper.deleteById(giftBoxId);
            //刪除直送箱商品關係表中的資料
            giftBoxProductMapper.deleteBySetmealId(giftBoxId);
        });
    }

    /**
     * 根據id查詢直送箱和直送箱商品關係
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
     * 修改直送箱
     *
     * @param setmealDTO
     */
    @Transactional
    public void update(GiftBoxDTO setmealDTO) {
        GiftBox setmeal = new GiftBox();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        //1、修改直送箱表，执行update
        giftBoxMapper.update(setmeal);

        //直送箱id
        Long giftBoxId = setmealDTO.getId();

        //2、刪除直送箱和商品的關聯關係，操作setmeal_dish表，执行delete
        giftBoxProductMapper.deleteBySetmealId(giftBoxId);

        List<GiftBoxProduct> giftBoxProducts = setmealDTO.getGiftBoxProducts();
        giftBoxProducts.forEach(setmealDish -> {
            setmealDish.setGiftBoxId(giftBoxId);
        });
        //3、重新插入直送箱和商品的關聯關係，操作setmeal_dish表，执行insert
        giftBoxProductMapper.insertBatch(giftBoxProducts);
    }

    /**
     * 直送箱起售、停售
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id) {
        //起售直送箱時，判断直送箱内是否有停售商品，有停售商品提示"直送箱内包含未啟售商品，無法啟售"
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
     * 條件查詢
     * @param setmeal
     * @return
     */
    public List<GiftBox> list(GiftBox setmeal) {
        List<GiftBox> list = giftBoxMapper.list(setmeal);
        return list;
    }

    /**
     * 根據id查詢商品选项
     * @param id
     * @return
     */
    public List<ProductItemVO> getDishItemById(Long id) {
        return giftBoxMapper.getDishItemBySetmealId(id);
    }

}
