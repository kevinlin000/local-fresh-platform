package com.localfresh.client.oauth;

public interface GoogleOAuthClient {

    GoogleProfile fetchProfile(String code, String redirectUri);
}
