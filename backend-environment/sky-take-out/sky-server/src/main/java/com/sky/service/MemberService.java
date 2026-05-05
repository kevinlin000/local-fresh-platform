package com.sky.service;

import com.sky.dto.MemberLoginDTO;
import com.sky.dto.GoogleOAuthLoginDTO;
import com.sky.vo.MemberLoginVO;

public interface MemberService {

    /**
     * 假登入
     *
     * @param memberLoginDTO
     * @return
     */
    MemberLoginVO mockLogin(MemberLoginDTO memberLoginDTO);

    MemberLoginVO googleOAuthLogin(GoogleOAuthLoginDTO googleOAuthLoginDTO);
}
