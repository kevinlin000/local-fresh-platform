package com.localfresh.config;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DeploymentInfoContributor implements InfoContributor {

    private static final String UNKNOWN = "unknown";

    private final Environment environment;

    public DeploymentInfoContributor(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void contribute(Info.Builder builder) {
        Map<String, Object> deployment = new LinkedHashMap<>();
        deployment.put("application", "local-fresh-server");
        deployment.put("profiles", environment.getActiveProfiles());
        deployment.put("commit", firstConfiguredValue("localfresh.deployment.commit",
                "SOURCE_COMMIT", "GIT_COMMIT", "GITHUB_SHA"));
        deployment.put("branch", firstConfiguredValue("localfresh.deployment.branch",
                "SOURCE_BRANCH", "GIT_BRANCH", "GITHUB_REF_NAME"));
        deployment.put("paymentCallbackPath", "/payment/callback");
        builder.withDetail("deployment", deployment);
    }

    private String firstConfiguredValue(String... propertyNames) {
        for (String propertyName : propertyNames) {
            String value = environment.getProperty(propertyName);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return UNKNOWN;
    }
}
