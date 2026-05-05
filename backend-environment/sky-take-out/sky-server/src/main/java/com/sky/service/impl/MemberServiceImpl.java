package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.MemberLoginDTO;
import com.sky.entity.Member;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.MemberMapper;
import com.sky.properties.JwtProperties;
import com.sky.service.MemberService;
import com.sky.utils.JwtUtil;
import com.sky.vo.MemberLoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class MemberServiceImpl implements MemberService {

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 假登入
     *
     * @param memberLoginDTO
     * @return
     */
    public MemberLoginVO mockLogin(MemberLoginDTO memberLoginDTO) {
        String code = memberLoginDTO.getCode();
        if (code == null || code.isBlank()) {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }

        String openid = "mock_" + code;
        log.info("會員假登入，code: {}, openid: {}", code, openid);

        Member member = memberMapper.selectByOpenid(openid);

        if (member == null) {
            member = Member.builder()
                    .openid(openid)
                    .name("測試會員_" + code)
                    .createTime(LocalDateTime.now())
                    .build();
            memberMapper.insert(member);
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, member.getId());
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);

        return MemberLoginVO.builder()
                .id(member.getId())
                .openid(member.getOpenid())
                .token(token)
                .build();
    }
}
