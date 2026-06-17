package com.localfresh.mapper;

import com.localfresh.entity.ShippingAddress;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ShippingAddressMapper {

    /**
     * 條件查詢
     * @param addressBook
     * @return
     */
    List<ShippingAddress> list(ShippingAddress addressBook);

    /**
     * 新增
     * @param addressBook
     */
    @Insert("insert into shipping_address" +
            "        (member_id, consignee, phone, sex, province_code, province_name, city_code, city_name, district_code," +
            "         district_name, detail, label, is_default)" +
            "        values (#{memberId}, #{consignee}, #{phone}, #{sex}, #{provinceCode}, #{provinceName}, #{cityCode}, #{cityName}," +
            "                #{districtCode}, #{districtName}, #{detail}, #{label}, #{isDefault})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ShippingAddress addressBook);

    /**
     * 根據id查詢
     * @param id
     * @return
     */
    @Select("select * from shipping_address where id = #{id}")
    ShippingAddress getById(Long id);

    /**
     * 根據id修改
     * @param addressBook
     */
    void update(ShippingAddress addressBook);

    /**
     * 根據 會員id修改 是否預設地址
     * @param addressBook
     */
    @Update("update shipping_address set is_default = #{isDefault} where member_id = #{memberId}")
    void updateIsDefaultByUserId(ShippingAddress addressBook);

    /**
     * 根據id刪除地址
     * @param id
     */
    @Delete("delete from shipping_address where id = #{id}")
    void deleteById(Long id);

}
