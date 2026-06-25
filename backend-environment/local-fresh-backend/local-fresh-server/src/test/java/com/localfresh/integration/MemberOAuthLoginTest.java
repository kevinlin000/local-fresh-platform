package com.localfresh.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.localfresh.client.oauth.GoogleOAuthClient;
import com.localfresh.client.oauth.GoogleProfile;
import com.localfresh.constant.MessageConstant;
import com.localfresh.entity.Member;
import com.localfresh.exception.LoginFailedException;
import com.localfresh.integration.support.MockWebSocketMvcIntegrationTest;
import com.localfresh.mapper.MemberMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MemberOAuthLoginTest extends MockWebSocketMvcIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberMapper memberMapper;

    @MockitoBean
    private GoogleOAuthClient googleOAuthClient;

    @Autowired
    private com.localfresh.properties.AuthProperties authProperties;

    @AfterEach
    void tearDown() {
        authProperties.setMockLoginEnabled(true);
    }

    @Test
    void passwordRegisterShouldCreateMemberAndReturnToken() throws Exception {
        mockMvc.perform(post("/user/member/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "fresh.member@example.com",
                                  "password": "FreshPass123",
                                  "name": "林小菜",
                                  "phone": "0912345678"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(1)))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.openid", is("fresh.member@example.com")))
                .andExpect(jsonPath("$.data.name", is("林小菜")))
                .andExpect(jsonPath("$.data.token").isString());

        Member member = memberMapper.selectByEmail("fresh.member@example.com");
        assertNotNull(member);
        assertNotNull(member.getPasswordHash());
        org.junit.jupiter.api.Assertions.assertNotEquals("FreshPass123", member.getPasswordHash());
    }

    @Test
    void passwordRegisterShouldRejectDuplicateEmail() throws Exception {
        memberMapper.insert(Member.builder()
                .email("duplicate@example.com")
                .name("既有會員")
                .passwordHash("$2a$10$existingHashForContractTest")
                .loginProvider("password")
                .createTime(LocalDateTime.now())
                .build());

        mockMvc.perform(post("/user/member/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "duplicate@example.com",
                                  "password": "FreshPass123",
                                  "name": "重複會員"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.msg", is("Email 已被註冊")));
    }

    @Test
    void passwordLoginShouldReturnTokenForRegisteredMember() throws Exception {
        mockMvc.perform(post("/user/member/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "login.member@example.com",
                                  "password": "FreshPass123",
                                  "name": "登入會員"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(1)));

        mockMvc.perform(post("/user/member/password-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "login.member@example.com",
                                  "password": "FreshPass123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(1)))
                .andExpect(jsonPath("$.data.openid", is("login.member@example.com")))
                .andExpect(jsonPath("$.data.name", is("登入會員")))
                .andExpect(jsonPath("$.data.token").isString());
    }

    @Test
    void passwordLoginShouldRejectWrongPassword() throws Exception {
        mockMvc.perform(post("/user/member/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "wrong-password@example.com",
                                  "password": "FreshPass123",
                                  "name": "登入失敗會員"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(1)));

        mockMvc.perform(post("/user/member/password-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "wrong-password@example.com",
                                  "password": "BadPass123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.msg", is(MessageConstant.PASSWORD_ERROR)));
    }

    @Test
    void googleOAuthShouldCreateNewMember() throws Exception {
        when(googleOAuthClient.fetchProfile(anyString(), anyString()))
                .thenReturn(GoogleProfile.builder()
                        .sub("google-sub-001")
                        .email("new-member@example.com")
                        .name("Google 新會員")
                        .picture("https://example.com/avatar.png")
                        .build());

        mockMvc.perform(post("/user/member/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OAuthLoginRequest("oauth-code", "http://localhost:5173/oauth/callback"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(1)))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.openid", is("google-sub-001")))
                .andExpect(jsonPath("$.data.token").isString());
    }

    @Test
    void googleOAuthShouldBindExistingMemberByEmail() throws Exception {
        memberMapper.insert(Member.builder()
                .openid("mock_existing")
                .email("existing@example.com")
                .name("既有會員")
                .loginProvider("mock")
                .createTime(LocalDateTime.now())
                .build());

        when(googleOAuthClient.fetchProfile(anyString(), anyString()))
                .thenReturn(GoogleProfile.builder()
                        .sub("google-sub-002")
                        .email("existing@example.com")
                        .name("Google 既有會員")
                        .picture("https://example.com/existing.png")
                        .build());

        mockMvc.perform(post("/user/member/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OAuthLoginRequest("oauth-code", "http://localhost:5173/oauth/callback"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(1)))
                .andExpect(jsonPath("$.data.openid", is("mock_existing")));

        Member updated = memberMapper.selectByGoogleSub("google-sub-002");
        assertNotNull(updated);
        assertEquals("mock_existing", updated.getOpenid());
        assertEquals("google", updated.getLoginProvider());
    }

    @Test
    void googleOAuthShouldReuseExistingMemberByGoogleSub() throws Exception {
        memberMapper.insert(Member.builder()
                .googleSub("google-sub-003")
                .email("sub-existing@example.com")
                .name("既有 Google 會員")
                .loginProvider("google")
                .createTime(LocalDateTime.now())
                .build());

        when(googleOAuthClient.fetchProfile(anyString(), anyString()))
                .thenReturn(GoogleProfile.builder()
                        .sub("google-sub-003")
                        .email("changed@example.com")
                        .name("Google 更新會員")
                        .picture("https://example.com/reuse.png")
                        .build());

        mockMvc.perform(post("/user/member/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OAuthLoginRequest("oauth-code", "http://localhost:5173/oauth/callback"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(1)))
                .andExpect(jsonPath("$.data.openid", is("google-sub-003")));
    }

    @Test
    void mockLoginShouldBeRejectedWhenDisabled() throws Exception {
        authProperties.setMockLoginEnabled(false);

        mockMvc.perform(post("/user/member/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"user_a\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.msg", is("此登入方式已停用")));
    }

    @Test
    void mockLoginShouldRejectBlankCode() throws Exception {
        mockMvc.perform(post("/user/member/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(0)));
    }

    @Test
    void googleOAuthShouldRejectBlankCode() throws Exception {
        mockMvc.perform(post("/user/member/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OAuthLoginRequest("", "http://localhost:5173/oauth/callback"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(0)));
    }

    @Test
    void googleOAuthShouldReturnErrorWhenTokenInvalid() throws Exception {
        when(googleOAuthClient.fetchProfile(anyString(), anyString()))
                .thenThrow(new LoginFailedException(MessageConstant.GOOGLE_OAUTH_TOKEN_INVALID));

        mockMvc.perform(post("/user/member/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OAuthLoginRequest("oauth-code", "http://localhost:5173/oauth/callback"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(0)))
                .andExpect(jsonPath("$.msg", is(MessageConstant.GOOGLE_OAUTH_TOKEN_INVALID)));
    }

    private static class OAuthLoginRequest {
        private final String code;
        private final String redirectUri;

        private OAuthLoginRequest(String code, String redirectUri) {
            this.code = code;
            this.redirectUri = redirectUri;
        }

        public String getCode() {
            return code;
        }

        public String getRedirectUri() {
            return redirectUri;
        }
    }
}
