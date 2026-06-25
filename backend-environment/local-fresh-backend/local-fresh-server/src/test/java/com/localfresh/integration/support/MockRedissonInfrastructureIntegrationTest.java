package com.localfresh.integration.support;

import org.redisson.api.RedissonClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public abstract class MockRedissonInfrastructureIntegrationTest extends MockInfrastructureIntegrationTest {

    @MockitoBean
    protected RedissonClient redissonClient;
}
