package com.localfresh.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "localfresh.auth")
@Data
public class AuthProperties {

    private boolean mockLoginEnabled = true;
}
