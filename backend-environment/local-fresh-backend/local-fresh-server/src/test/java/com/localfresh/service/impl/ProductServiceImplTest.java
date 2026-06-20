package com.localfresh.service.impl;

import com.localfresh.constant.MessageConstant;
import com.localfresh.constant.StatusConstant;
import com.localfresh.context.BaseContext;
import com.localfresh.dto.ProductInventoryAdjustDTO;
import com.localfresh.entity.GiftBox;
import com.localfresh.entity.Product;
import com.localfresh.exception.DeletionNotAllowedException;
import com.localfresh.mapper.GiftBoxMapper;
import com.localfresh.mapper.GiftBoxProductMapper;
import com.localfresh.mapper.ProductInventoryLogMapper;
import com.localfresh.mapper.ProductMapper;
import com.localfresh.mapper.ProductSpecMapper;
import com.localfresh.service.AdminOperationLogService;
import com.localfresh.service.CacheService;
import com.localfresh.service.InventoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private GiftBoxMapper giftBoxMapper;

    @Mock
    private ProductSpecMapper productSpecMapper;

    @Mock
    private GiftBoxProductMapper giftBoxProductMapper;

    @Mock
    private ProductInventoryLogMapper productInventoryLogMapper;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private CacheService cacheService;

    @Mock
    private AdminOperationLogService adminOperationLogService;

    @InjectMocks
    private ProductServiceImpl productService;

    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
    }

    @Test
    void adjustInventoryShouldWriteAdminAuditLogWithStockBeforeAndAfter() {
        BaseContext.setCurrentId(77L);
        Product before = Product.builder().id(9L).stock(10).build();
        Product after = Product.builder().id(9L).stock(15).build();
        when(productMapper.getById(9L)).thenReturn(before, after);

        ProductInventoryAdjustDTO dto = new ProductInventoryAdjustDTO();
        dto.setChangeQuantity(5);
        dto.setReason("補貨");

        productService.adjustInventory(9L, dto);

        verify(inventoryService).adjustProduct(9L, 5, "MANUAL_ADJUSTMENT", "補貨", "ADMIN", 77L);
        verify(adminOperationLogService).recordProductInventoryAdjustment(9L, 10, 15, "補貨");
        verify(cacheService).evictByPattern("product_*");
    }

    @Test
    void deleteBatchShouldRejectEnabledProductBeforeDeletingAnything() {
        when(productMapper.getById(9L)).thenReturn(Product.builder()
                .id(9L)
                .status(StatusConstant.ENABLE)
                .build());

        DeletionNotAllowedException exception = assertThrows(DeletionNotAllowedException.class,
                () -> productService.deleteBatch(List.of(9L)));

        assertEquals(MessageConstant.DISH_ON_SALE, exception.getMessage());
        verify(productMapper, never()).deleteByIds(any());
        verifyNoInteractions(giftBoxProductMapper, productSpecMapper, cacheService);
    }

    @Test
    void deleteBatchShouldRejectProductReferencedByGiftBox() {
        when(productMapper.getById(9L)).thenReturn(Product.builder()
                .id(9L)
                .status(StatusConstant.DISABLE)
                .build());
        when(giftBoxProductMapper.getSetmealIdsByDishIds(List.of(9L))).thenReturn(List.of(88L));

        DeletionNotAllowedException exception = assertThrows(DeletionNotAllowedException.class,
                () -> productService.deleteBatch(List.of(9L)));

        assertEquals(MessageConstant.DISH_BE_RELATED_BY_SETMEAL, exception.getMessage());
        verify(productMapper, never()).deleteByIds(any());
        verify(productSpecMapper, never()).deleteByDishIds(any());
        verifyNoInteractions(cacheService);
    }

    @Test
    void deleteBatchShouldDeleteDisabledProductsSpecsAndEvictProductCache() {
        List<Long> ids = List.of(9L, 10L);
        when(productMapper.getById(9L)).thenReturn(Product.builder()
                .id(9L)
                .status(StatusConstant.DISABLE)
                .build());
        when(productMapper.getById(10L)).thenReturn(Product.builder()
                .id(10L)
                .status(StatusConstant.DISABLE)
                .build());
        when(giftBoxProductMapper.getSetmealIdsByDishIds(ids)).thenReturn(List.of());

        productService.deleteBatch(ids);

        verify(productMapper).deleteByIds(ids);
        verify(productSpecMapper).deleteByDishIds(ids);
        verify(cacheService).evictByPattern("product_*");
    }

    @Test
    void startOrStopShouldDisableRelatedGiftBoxesWhenProductIsDisabled() {
        when(giftBoxProductMapper.getSetmealIdsByDishIds(List.of(9L))).thenReturn(List.of(88L, 89L));

        productService.startOrStop(StatusConstant.DISABLE, 9L);

        Product updatedProduct = captureUpdatedProduct();
        assertEquals(9L, updatedProduct.getId());
        assertEquals(StatusConstant.DISABLE, updatedProduct.getStatus());

        ArgumentCaptor<GiftBox> giftBoxCaptor = ArgumentCaptor.forClass(GiftBox.class);
        verify(giftBoxMapper, times(2)).update(giftBoxCaptor.capture());
        List<GiftBox> updatedGiftBoxes = giftBoxCaptor.getAllValues();
        assertEquals(List.of(88L, 89L), updatedGiftBoxes.stream().map(GiftBox::getId).toList());
        assertEquals(List.of(StatusConstant.DISABLE, StatusConstant.DISABLE),
                updatedGiftBoxes.stream().map(GiftBox::getStatus).toList());
        verify(cacheService).evictByPattern("product_*");
    }

    @Test
    void startOrStopShouldNotTouchGiftBoxesWhenProductIsEnabled() {
        productService.startOrStop(StatusConstant.ENABLE, 9L);

        Product updatedProduct = captureUpdatedProduct();
        assertEquals(9L, updatedProduct.getId());
        assertEquals(StatusConstant.ENABLE, updatedProduct.getStatus());
        verifyNoInteractions(giftBoxProductMapper, giftBoxMapper);
        verify(cacheService).evictByPattern("product_*");
    }

    private Product captureUpdatedProduct() {
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).update(captor.capture());
        return captor.getValue();
    }
}
