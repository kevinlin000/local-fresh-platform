package com.localfresh.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.info.Info;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DeploymentInfoContributorTest {

    @Test
    void contributeShouldExposeDeploymentIdentityFromConfiguredProperties() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.profiles.active", "prod")
                .withProperty("localfresh.deployment.commit", "42c12ce")
                .withProperty("localfresh.deployment.branch", "hardening-and-upgrade");
        environment.setActiveProfiles("prod");
        DeploymentInfoContributor contributor = new DeploymentInfoContributor(environment);

        Info.Builder builder = new Info.Builder();
        contributor.contribute(builder);

        Map<String, Object> deployment = deploymentDetails(builder.build());
        assertEquals("local-fresh-server", deployment.get("application"));
        assertArrayEquals(new String[]{"prod"}, (String[]) deployment.get("profiles"));
        assertEquals("42c12ce", deployment.get("commit"));
        assertEquals("hardening-and-upgrade", deployment.get("branch"));
        assertEquals("/payment/callback", deployment.get("paymentCallbackPath"));
    }

    @Test
    void contributeShouldFallbackToEnvironmentVariablesAndUnknownDefaults() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("SOURCE_COMMIT", "source-commit");
        DeploymentInfoContributor contributor = new DeploymentInfoContributor(environment);

        Info.Builder builder = new Info.Builder();
        contributor.contribute(builder);

        Map<String, Object> deployment = deploymentDetails(builder.build());
        assertEquals("source-commit", deployment.get("commit"));
        assertEquals("unknown", deployment.get("branch"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deploymentDetails(Info info) {
        return (Map<String, Object>) info.getDetails().get("deployment");
    }
}
