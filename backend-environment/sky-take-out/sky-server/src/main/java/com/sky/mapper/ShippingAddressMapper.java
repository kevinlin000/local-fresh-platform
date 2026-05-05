package com.sky.mapper;

import com.sky.entity.ShippingAddress;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ShippingAddressMapper {

    /**
     * 条件查询
     * @param addressBook
     * @return
     */
    List<ShippingAddress> list(ShippingAddress addressBook);

    /**
     * 新增
     * @param addressBook
     */
    @Insert("insert into address_book" +
            "        (user_id, consignee, phone, sex, province_code, province_name, city_code, city_name, district_code," +
            "         district_name, detail, label, is_default)" +
            "        values (#{userId}, #{consignee}, #{phone}, #{sex}, #{provinceCode}, #{provinceName}, #{cityCode}, #{cityName}," +
            "                #{districtCode}, #{districtName}, #{detail}, #{label}, #{isDefault})")
    void insert(ShippingAddress addressBook);

    /**
     * 根据id查询
     * @param id
     * @return
     */
    @Select("select * from address_book where id = #{id}")
    ShippingAddress getById(Long id);

    /**
     * 根据id修改
     * @param addressBook
     */
    void update(ShippingAddress addressBook);

    /**
     * 根据 用户id修改 是否默认地址
     * @param addressBook
     */
    @Update("update address_book set is_default = #{isDefault} where user_id = #{userId}")
    void updateIsDefaultByUserId(ShippingAddress addressBook);

    /**
     * 根据id删除地址
     * @param id
     */
    @Delete("delete from address_book where id = #{id}")
    void deleteById(Long id);

}
