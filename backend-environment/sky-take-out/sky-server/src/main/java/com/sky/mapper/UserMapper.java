package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {
    /**
     * 根據openid查詢用戶
     * @param openid
     * @return
     */
    @Select("select * from user where openid = #{openid}")
    User selectByOpenid(String openid);

    /**
     * 插入數據
     * @param user
     */
    void insert(User user);

    @Select("select * from user where id = #{userId}")
    User getById(Long userId);
}
