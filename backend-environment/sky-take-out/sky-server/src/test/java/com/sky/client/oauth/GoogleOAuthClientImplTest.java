package com.sky.client.oauth;

import com.sky.exception.LoginFailedException;
import com.sky.properties.GoogleOAuthProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class GoogleOAuthClientImplTest {

    @Test
    void fetchProfileShouldRejectUntrustedRedirectUriBeforeCallingGoogle() {
        GoogleOAuthProperties properties = new GoogleOAuthProperties();
        properties.setClientId("test-client-id");
        properties.setClientSecret("test-client-secret");
        properties.setRedirectUri("http://localhost:5173/oauth/callback");

        GoogleOAuthClientImpl client = new GoogleOAuthClientImpl(properties);

        assertThrows(LoginFailedException.class,
                () -> client.fetchProfile("oauth-code", "https://evil.example.com/oauth/callback"));
    }
}
