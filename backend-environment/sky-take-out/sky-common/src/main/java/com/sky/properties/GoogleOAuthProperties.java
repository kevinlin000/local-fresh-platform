package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sky.oauth.google")
@Data
public class GoogleOAuthProperties {

    private String clientId;
    private String clientSecret;
    private String redirectUri;
}
