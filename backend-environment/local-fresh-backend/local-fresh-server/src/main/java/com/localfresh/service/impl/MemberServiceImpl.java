package com.localfresh.service.impl;

import com.localfresh.client.oauth.GoogleOAuthClient;
import com.localfresh.client.oauth.GoogleProfile;
import com.localfresh.constant.MessageConstant;
import com.localfresh.constant.JwtClaimsConstant;
import com.localfresh.dto.GoogleOAuthLoginDTO;
import com.localfresh.dto.MemberLoginDTO;
import com.localfresh.entity.Member;
import com.localfresh.exception.LoginFailedException;
import com.localfresh.mapper.MemberMapper;
import com.localfresh.properties.AuthProperties;
import com.localfresh.properties.JwtProperties;
import com.localfresh.service.MemberService;
import com.localfresh.utils.JwtUtil;
import com.localfresh.vo.MemberLoginVO;
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

    @Autowired
    private AuthProperties authProperties;

    @Autowired
    private GoogleOAuthClient googleOAuthClient;

    /**
     * 假登入
     *
     * @param memberLoginDTO
     * @return
     */
    public MemberLoginVO mockLogin(MemberLoginDTO memberLoginDTO) {
        if (!authProperties.isMockLoginEnabled()) {
            throw new LoginFailedException(MessageConstant.LOGIN_DISABLED);
        }

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
                    .name("試用會員_" + code)
                    .loginProvider("mock")
                    .createTime(LocalDateTime.now())
                    .build();
            memberMapper.insert(member);
        }

        return buildLoginVO(member);
    }

    @Override
    public MemberLoginVO googleOAuthLogin(GoogleOAuthLoginDTO googleOAuthLoginDTO) {
        if (googleOAuthLoginDTO.getCode() == null || googleOAuthLoginDTO.getCode().isBlank()) {
            throw new LoginFailedException(MessageConstant.GOOGLE_OAUTH_FAILED);
        }

        GoogleProfile googleProfile = googleOAuthClient.fetchProfile(
                googleOAuthLoginDTO.getCode(),
                googleOAuthLoginDTO.getRedirectUri()
        );

        Member member = memberMapper.selectByGoogleSub(googleProfile.getSub());
        if (member == null && googleProfile.getEmail() != null && !googleProfile.getEmail().isBlank()) {
            member = memberMapper.selectByEmail(googleProfile.getEmail());
        }

        if (member == null) {
            member = Member.builder()
                    .googleSub(googleProfile.getSub())
                    .email(googleProfile.getEmail())
                    .name(googleProfile.getName())
                    .avatarUrl(googleProfile.getPicture())
                    .loginProvider("google")
                    .createTime(LocalDateTime.now())
                    .build();
            memberMapper.insert(member);
        } else {
            member.setGoogleSub(googleProfile.getSub());
            member.setEmail(googleProfile.getEmail());
            member.setName(googleProfile.getName());
            member.setAvatarUrl(googleProfile.getPicture());
            member.setLoginProvider("google");
            memberMapper.updateOAuthInfo(member);
        }

        return buildLoginVO(member);
    }

    private MemberLoginVO buildLoginVO(Member member) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, member.getId());
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);

        return MemberLoginVO.builder()
                .id(member.getId())
                .openid(member.getOpenid() != null ? member.getOpenid() : member.getGoogleSub())
                .name(member.getName())
                .token(token)
                .build();
    }
}
