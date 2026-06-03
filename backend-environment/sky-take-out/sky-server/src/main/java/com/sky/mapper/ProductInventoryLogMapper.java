package com.sky.mapper;

import com.sky.entity.ProductInventoryLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductInventoryLogMapper {

    @Insert("insert into product_inventory_log " +
            "(product_id, change_quantity, stock_before, stock_after, reason, remark, reference_type, reference_id, operator_type, operator_id, created_at) " +
            "values (#{productId}, #{changeQuantity}, #{stockBefore}, #{stockAfter}, #{reason}, #{remark}, #{referenceType}, #{referenceId}, #{operatorType}, #{operatorId}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ProductInventoryLog productInventoryLog);

    @Select("select * from product_inventory_log where product_id = #{productId} order by created_at asc, id asc")
    List<ProductInventoryLog> listByProductId(Long productId);
}
