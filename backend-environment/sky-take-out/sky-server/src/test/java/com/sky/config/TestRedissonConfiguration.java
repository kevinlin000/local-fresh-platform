package com.sky.config;

import org.mockito.Mockito;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class TestRedissonConfiguration {

    @Bean
    @ConditionalOnMissingBean(RedissonClient.class)
    @ConditionalOnProperty(name = "sky.test.mock-redisson", havingValue = "true", matchIfMissing = true)
    public RedissonClient testRedissonClient() {
        return Mockito.mock(RedissonClient.class);
    }
}
