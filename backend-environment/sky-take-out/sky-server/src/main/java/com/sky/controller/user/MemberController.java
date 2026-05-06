package com.sky.controller.user;

import com.sky.dto.GoogleOAuthLoginDTO;
import com.sky.dto.MemberLoginDTO;
import com.sky.result.Result;
import com.sky.service.MemberService;
import com.sky.vo.MemberLoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/user/member")
@Api(tags = "會員相關介面")
@Slf4j
public class MemberController {

    @Autowired
    private MemberService memberService;
    /**
     * 會員登入
     * @param userLoginDTO
     * @return
     */
    @PostMapping("/login")
    @ApiOperation("會員登入")
    public Result<MemberLoginVO> login(@RequestBody MemberLoginDTO userLoginDTO) {
        log.info("會員登入：{}", userLoginDTO.getCode());
        return Result.success(memberService.mockLogin(userLoginDTO));

    }

    @PostMapping("/oauth/google")
    @ApiOperation("Google OAuth 會員登入")
    public Result<MemberLoginVO> googleOAuthLogin(@RequestBody GoogleOAuthLoginDTO googleOAuthLoginDTO) {
        log.info("Google OAuth 會員登入，redirectUri={}", googleOAuthLoginDTO.getRedirectUri());
        return Result.success(memberService.googleOAuthLogin(googleOAuthLoginDTO));
    }
}
