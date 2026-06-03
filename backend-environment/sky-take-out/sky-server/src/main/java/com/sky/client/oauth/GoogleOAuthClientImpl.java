package com.sky.client.oauth;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.sky.constant.MessageConstant;
import com.sky.exception.LoginFailedException;
import com.sky.properties.GoogleOAuthProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.GeneralSecurityException;
import java.util.Collections;

@Component
@Slf4j
public class GoogleOAuthClientImpl implements GoogleOAuthClient {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private final GoogleOAuthProperties googleOAuthProperties;

    public GoogleOAuthClientImpl(GoogleOAuthProperties googleOAuthProperties) {
        this.googleOAuthProperties = googleOAuthProperties;
    }

    @Override
    public GoogleProfile fetchProfile(String code, String redirectUri) {
        try {
            HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
            String resolvedRedirectUri = (redirectUri == null || redirectUri.isBlank())
                    ? googleOAuthProperties.getRedirectUri()
                    : redirectUri;
            if (!googleOAuthProperties.getRedirectUri().equals(resolvedRedirectUri)) {
                throw new LoginFailedException(MessageConstant.GOOGLE_OAUTH_FAILED);
            }

            GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                    transport,
                    JSON_FACTORY,
                    "https://oauth2.googleapis.com/token",
                    googleOAuthProperties.getClientId(),
                    googleOAuthProperties.getClientSecret(),
                    code,
                    resolvedRedirectUri
            ).execute();

            GoogleIdToken.Payload payload = verifyIdToken(transport, tokenResponse.getIdToken());

            return GoogleProfile.builder()
                    .sub(payload.getSubject())
                    .email(payload.getEmail())
                    .name((String) payload.get("name"))
                    .picture((String) payload.get("picture"))
                    .build();
        } catch (LoginFailedException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Google OAuth login failed", ex);
            throw new LoginFailedException(MessageConstant.GOOGLE_OAUTH_FAILED);
        }
    }

    private GoogleIdToken.Payload verifyIdToken(HttpTransport transport, String idTokenValue)
            throws GeneralSecurityException, java.io.IOException {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, JSON_FACTORY)
                .setAudience(Collections.singletonList(googleOAuthProperties.getClientId()))
                .build();

        GoogleIdToken verifiedToken = verifier.verify(idTokenValue);
        if (verifiedToken == null) {
            throw new LoginFailedException(MessageConstant.GOOGLE_OAUTH_TOKEN_INVALID);
        }

        return verifiedToken.getPayload();
    }
}
