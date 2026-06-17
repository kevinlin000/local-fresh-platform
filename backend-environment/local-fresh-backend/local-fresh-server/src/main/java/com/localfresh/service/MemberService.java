package com.localfresh.service;

import com.localfresh.dto.MemberLoginDTO;
import com.localfresh.dto.GoogleOAuthLoginDTO;
import com.localfresh.vo.MemberLoginVO;

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
