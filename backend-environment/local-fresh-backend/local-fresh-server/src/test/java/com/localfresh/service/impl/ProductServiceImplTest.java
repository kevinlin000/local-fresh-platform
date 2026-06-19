package com.localfresh.service.impl;

import com.localfresh.context.BaseContext;
import com.localfresh.dto.ProductInventoryAdjustDTO;
import com.localfresh.entity.Product;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
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
}
