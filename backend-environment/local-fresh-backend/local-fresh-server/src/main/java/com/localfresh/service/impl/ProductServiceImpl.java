package com.localfresh.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.localfresh.constant.MessageConstant;
import com.localfresh.constant.StatusConstant;
import com.localfresh.context.BaseContext;
import com.localfresh.dto.ProductDTO;
import com.localfresh.dto.ProductInventoryAdjustDTO;
import com.localfresh.dto.ProductPageQueryDTO;
import com.localfresh.entity.Product;
import com.localfresh.entity.ProductSpec;
import com.localfresh.entity.GiftBox;
import com.localfresh.entity.ProductInventoryLog;
import com.localfresh.exception.DeletionNotAllowedException;
import com.localfresh.exception.BaseException;
import com.localfresh.mapper.ProductInventoryLogMapper;
import com.localfresh.mapper.ProductSpecMapper;
import com.localfresh.mapper.ProductMapper;
import com.localfresh.mapper.GiftBoxProductMapper;
import com.localfresh.mapper.GiftBoxMapper;
import com.localfresh.result.PageResult;
import com.localfresh.service.AdminOperationLogService;
import com.localfresh.service.CacheService;
import com.localfresh.service.InventoryService;
import com.localfresh.service.ProductService;
import com.localfresh.vo.ProductInventoryLogVO;
import com.localfresh.vo.ProductVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService {

    private static final int DEFAULT_STOCK = 100;
    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 10;
    private static final String INVENTORY_REASON_MANUAL_ADJUSTMENT = "MANUAL_ADJUSTMENT";
    private static final String INVENTORY_OPERATOR_ADMIN = "ADMIN";

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private GiftBoxMapper giftBoxMapper;
    @Autowired
    private ProductSpecMapper productSpecMapper;
    @Autowired
    private GiftBoxProductMapper giftBoxProductMapper;
    @Autowired
    private ProductInventoryLogMapper productInventoryLogMapper;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private CacheService cacheService;
    @Autowired
    private AdminOperationLogService adminOperationLogService;
    /**
     * 新增商品
     * @param dishDTO
     * @return
     */
    @Transactional
    public void saveWithFlavor(com.localfresh.dto.ProductDTO dishDTO) {

        Product dish = new Product();
        BeanUtils.copyProperties(dishDTO, dish);
        applyInventoryDefaults(dish);

        //向商品表插入一條資料
        productMapper.insert(dish);

        //獲取inset語句所產生的主鍵值
        Long productId = dish.getId();

        //向規格表插入n條資料
        List<ProductSpec> productSpecs = dishDTO.getProductSpecs();
        if(productSpecs!=null&&productSpecs.size()>0){
            productSpecs.forEach(productSpec->{
                productSpec.setProductId(productId);
            });
            //向規格表插入n條資料
            productSpecMapper.insertBatch(productSpecs);
        }

        cleanCache("product_" + dishDTO.getCategoryId());
    }

    /**
     * 商品分頁查詢
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(ProductPageQueryDTO dishPageQueryDTO)  {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<ProductVO> page= productMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());

        }

    /**
     * 商品的批次刪除
     * @param ids
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //判斷當前商品是否能夠刪除 - 是否存在上架中的商品？
        for (Long id : ids) {
            Product dish = productMapper.getById(id);
            if (dish.getStatus() == StatusConstant.ENABLE) {
                //當前商品正在上架中，無法刪除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        //判斷當前商品是否能夠刪除 -是否被直送箱關聯了？
        List<Long> setmealIds = giftBoxProductMapper.getSetmealIdsByDishIds(ids);
        if(setmealIds!=null&&setmealIds.size()>0){
            // 當前商品被直送箱關聯了，不能刪除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }


        //根據商品id集合批次刪除商品資料
        //sql: delete from dish where id in (?,?,?)
        productMapper.deleteByIds(ids);
        //根據商品id集合批次刪除關聯的規格資料
        // 刪除商品既有規格資料
        productSpecMapper.deleteByDishIds(ids);

        cleanCache("product_*");
    }

    /**
     *  根據id查詢商品和對應的規格
     * @param id
     * @return
     */

    public ProductVO getByIdWithFlavor(Long id) {
        //根據id查詢商品資料
        Product dish = productMapper.getById(id);
        //根據商品id查詢規格資料
        List<ProductSpec> dishFlavors = productSpecMapper.getByDishId(id);

        //講查詢到的資料封裝到ProductVO
        ProductVO dishVO = new ProductVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setProductSpecs(dishFlavors);

        return dishVO;
    }

    /**
     * 根據id修改商品基本資訊和對應的規格資訊
     * @param dishDTO
     */

    public void updateWithFlavor(ProductDTO dishDTO) {
        Product dish = new Product();
        BeanUtils.copyProperties(dishDTO, dish);
        applyInventoryDefaults(dish);
        //修改商品表基本資訊
        productMapper.update(dish);

        //刪除原有的規格資訊
        productSpecMapper.deleteByDishId(dishDTO.getId());

        //重新插入規格資訊
        List<ProductSpec> productSpecs = dishDTO.getProductSpecs();
        if(productSpecs!=null && productSpecs.size()>0){
            productSpecs.forEach(productSpec->{
                productSpec.setProductId(dishDTO.getId());
            });
            //向規格表插入n條資料
            productSpecMapper.insertBatch(productSpecs);
        }

        cleanCache("product_*");
    }


    /**
     * 根據分類id查詢商品
     * @param categoryId
     * @return
     */
    public List<Product> list(Long categoryId) {
        Product dish = Product.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        return productMapper.list(dish);
    }

    /**
     * 條件查詢商品和規格
     * @param dish
     * @return
     */
    public List<ProductVO> listWithFlavor(Product dish) {
        List<Product> dishList = productMapper.list(dish);

        List<ProductVO> dishVOList = new ArrayList<>();

        for (Product d : dishList) {
            ProductVO dishVO = new ProductVO();
            BeanUtils.copyProperties(d,dishVO);

            //根據商品id查詢對應的規格
            List<ProductSpec> productSpecs = productSpecMapper.getByDishId(d.getId());

            dishVO.setProductSpecs(productSpecs);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }

    /**
     * 商品上架下架
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
        productMapper.update(dish);

        if (status == StatusConstant.DISABLE) {
            // 如果是下架操作，還需要將包含目前商品的直送箱也下架
            List<Long> dishIds = new ArrayList<>();
            dishIds.add(id);
            // 查詢包含該商品的直送箱，下架時一併下架
            List<Long> setmealIds = giftBoxProductMapper.getSetmealIdsByDishIds(dishIds);
            if (setmealIds != null && setmealIds.size() > 0) {
                for (Long giftBoxId : setmealIds) {
                    GiftBox setmeal = GiftBox.builder()
                            .id(giftBoxId)
                            .status(StatusConstant.DISABLE)
                            .build();
                    giftBoxMapper.update(setmeal);
                }
            }
        }

        cleanCache("product_*");
    }

    @Override
    public List<ProductInventoryLogVO> listInventoryLogs(Long id) {
        Product product = productMapper.getById(id);
        if (product == null) {
            throw new BaseException(MessageConstant.PRODUCT_NOT_AVAILABLE);
        }

        return productInventoryLogMapper.listByProductId(id).stream()
                .map(this::toInventoryLogVO)
                .toList();
    }

    @Override
    @Transactional
    public void adjustInventory(Long id, ProductInventoryAdjustDTO productInventoryAdjustDTO) {
        Product before = productMapper.getById(id);

        inventoryService.adjustProduct(id, productInventoryAdjustDTO.getChangeQuantity(),
                INVENTORY_REASON_MANUAL_ADJUSTMENT, productInventoryAdjustDTO.getReason(),
                INVENTORY_OPERATOR_ADMIN, BaseContext.getCurrentId());

        Product after = productMapper.getById(id);
        adminOperationLogService.recordProductInventoryAdjustment(id,
                before == null ? null : before.getStock(),
                after == null ? null : after.getStock(),
                productInventoryAdjustDTO.getReason());

        cleanCache("product_*");
    }

    private void cleanCache(String pattern) {
        cacheService.evictByPattern(pattern);
    }

    private void applyInventoryDefaults(Product product) {
        if (product.getStock() == null) {
            product.setStock(DEFAULT_STOCK);
        }
        if (product.getLowStockThreshold() == null) {
            product.setLowStockThreshold(DEFAULT_LOW_STOCK_THRESHOLD);
        }
    }

    private ProductInventoryLogVO toInventoryLogVO(ProductInventoryLog log) {
        ProductInventoryLogVO vo = new ProductInventoryLogVO();
        BeanUtils.copyProperties(log, vo);
        return vo;
    }

}
