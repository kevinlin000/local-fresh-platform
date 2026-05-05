package com.sky.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.client.oauth.GoogleOAuthClient;
import com.sky.client.oauth.GoogleProfile;
import com.sky.entity.Member;
import com.sky.mapper.MemberMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MemberOAuthLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberMapper memberMapper;

    @MockBean
    private GoogleOAuthClient googleOAuthClient;

    @MockBean
    private ServerEndpointExporter serverEndpointExporter;

    @Autowired
    private com.sky.properties.AuthProperties authProperties;

    @AfterEach
    void tearDown() {
        authProperties.setMockLoginEnabled(true);
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
