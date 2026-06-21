package com.localfresh.config;

import com.localfresh.entity.Category;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MyBatisConfigurationTest {

    @Test
    void localFreshEntityAliasesShouldRegisterModelClasses() {
        Configuration configuration = new Configuration();

        new MyBatisConfiguration().localFreshEntityAliases().customize(configuration);

        assertEquals(Category.class, configuration.getTypeAliasRegistry().resolveAlias("Category"));
    }
}
