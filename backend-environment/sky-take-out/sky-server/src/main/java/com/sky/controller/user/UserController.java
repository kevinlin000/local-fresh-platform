package com.sky.controller.user;

import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.MemberLoginDTO;
import com.sky.entity.Member;
import com.sky.properties.JwtProperties;
import com.sky.result.Result;
import com.sky.service.MemberService;
import com.sky.utils.JwtUtil;
import com.sky.vo.MemberLoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/user/user")
@Api(tags = "用户相關接口")
@Slf4j
public class UserController {

    @Autowired
    private MemberService memberService;

    @Autowired
    private JwtProperties jwtProperties;
    /**
     * 微信登錄
     * @param userLoginDTO
     * @return
     */
    @PostMapping("/login")
    @ApiOperation("微信登錄")
    public Result<MemberLoginVO> login(@RequestBody MemberLoginDTO userLoginDTO) {
        log.info("微信用戶登錄：{}", userLoginDTO.getCode());

        // 微信登錄
        Member user = memberService.wxLogin(userLoginDTO);

        //為微信用戶生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);


        MemberLoginVO userLoginVO = MemberLoginVO.builder()
                .id(user.getId())
                .openid(user.getOpenid())
                .token(token)
                .build();

        return Result.success(userLoginVO);

    }
}
