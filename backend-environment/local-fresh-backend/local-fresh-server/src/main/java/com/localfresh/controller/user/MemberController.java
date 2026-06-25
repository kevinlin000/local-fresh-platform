package com.localfresh.controller.user;

import com.localfresh.dto.GoogleOAuthLoginDTO;
import com.localfresh.dto.MemberLoginDTO;
import com.localfresh.dto.MemberPasswordLoginDTO;
import com.localfresh.dto.MemberRegisterDTO;
import com.localfresh.result.Result;
import com.localfresh.service.MemberService;
import com.localfresh.vo.MemberLoginVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/user/member")
@Tag(name = "會員相關介面")
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
    @Operation(summary = "會員登入")
    public Result<MemberLoginVO> login(@Valid @RequestBody MemberLoginDTO userLoginDTO) {
        log.info("會員登入：{}", userLoginDTO.getCode());
        return Result.success(memberService.mockLogin(userLoginDTO));

    }

    @PostMapping("/register")
    @Operation(summary = "會員註冊")
    public Result<MemberLoginVO> register(@Valid @RequestBody MemberRegisterDTO memberRegisterDTO) {
        log.info("會員註冊：{}", memberRegisterDTO.getEmail());
        return Result.success(memberService.register(memberRegisterDTO));
    }

    @PostMapping("/password-login")
    @Operation(summary = "Email 密碼登入")
    public Result<MemberLoginVO> passwordLogin(@Valid @RequestBody MemberPasswordLoginDTO memberPasswordLoginDTO) {
        log.info("會員密碼登入：{}", memberPasswordLoginDTO.getEmail());
        return Result.success(memberService.passwordLogin(memberPasswordLoginDTO));
    }

    @PostMapping("/oauth/google")
    @Operation(summary = "Google OAuth 會員登入")
    public Result<MemberLoginVO> googleOAuthLogin(@Valid @RequestBody GoogleOAuthLoginDTO googleOAuthLoginDTO) {
        log.info("Google OAuth 會員登入，redirectUri={}", googleOAuthLoginDTO.getRedirectUri());
        return Result.success(memberService.googleOAuthLogin(googleOAuthLoginDTO));
    }
}
