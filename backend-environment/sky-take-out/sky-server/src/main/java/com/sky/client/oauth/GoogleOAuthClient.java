package com.sky.client.oauth;

public interface GoogleOAuthClient {

    GoogleProfile fetchProfile(String code, String redirectUri);
}
