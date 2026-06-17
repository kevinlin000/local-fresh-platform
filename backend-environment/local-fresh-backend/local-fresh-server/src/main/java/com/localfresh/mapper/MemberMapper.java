package com.localfresh.mapper;

import com.localfresh.entity.Member;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface MemberMapper {
    /**
     * 根據openid查詢用戶
     * @param openid
     * @return
     */
    @Select("select * from member where openid = #{openid}")
    Member selectByOpenid(String openid);

    @Select("select * from member where google_sub = #{googleSub}")
    Member selectByGoogleSub(String googleSub);

    @Select("select * from member where email = #{email}")
    Member selectByEmail(String email);

    /**
     * 插入數據
     * @param user
     */
    void insert(Member user);

    @Select("select * from member where id = #{userId}")
    Member getById(Long userId);


    /**
     * 根據動態條件來統計用戶數量
     * @param map
     * @return
     */
    Integer countByMap(Map map);

    void updateOAuthInfo(Member member);

}
