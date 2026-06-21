package com.localfresh.config;

import org.apache.ibatis.session.Configuration;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;

@org.springframework.context.annotation.Configuration
public class MyBatisConfiguration {

    @Bean
    public ConfigurationCustomizer localFreshEntityAliases() {
        return this::registerEntityAliases;
    }

    private void registerEntityAliases(Configuration configuration) {
        configuration.getTypeAliasRegistry().registerAliases("com.localfresh.entity");
    }
}
