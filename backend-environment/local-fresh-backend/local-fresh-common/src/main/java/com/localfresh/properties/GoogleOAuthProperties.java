package com.localfresh.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "localfresh.oauth.google")
@Data
public class GoogleOAuthProperties {

    private String clientId;
    private String clientSecret;
    private String redirectUri;
}
