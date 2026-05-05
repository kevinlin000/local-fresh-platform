package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sky.auth")
@Data
public class AuthProperties {

    private boolean mockLoginEnabled = true;
}
