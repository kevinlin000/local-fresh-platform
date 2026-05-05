package com.sky.test.support;

public class LoginResult {
    private final Long userId;
    private final String token;

    public LoginResult(Long userId, String token) {
        this.userId = userId;
        this.token = token;
    }

    public Long userId() { return userId; }
    public String token() { return token; }
}
