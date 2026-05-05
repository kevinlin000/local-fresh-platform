package com.sky.service;

import com.sky.dto.MemberLoginDTO;
import com.sky.entity.Member;

public interface MemberService {

    /**
     * 微信登錄
     * @param userLoginVO
     * @return
     */
    Member wxLogin(MemberLoginDTO userLoginVO);
}
