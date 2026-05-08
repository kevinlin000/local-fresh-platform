package com.sky.config;

import com.sky.properties.AwsS3Properties;
import com.sky.utils.AwsS3Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置類別，用於建立 AwsS3Util 物件
 */
@Configuration
@Slf4j
public class AwsS3Configuration {

    @Bean
    @ConditionalOnMissingBean // 確保 Spring 容器中只有一個這樣的 Bean
    public AwsS3Util awsS3Util(AwsS3Properties awsS3Properties) {
        log.info("AWS S3 初始化:region={}, bucket={}",
                awsS3Properties.getRegion(),
                awsS3Properties.getBucketName());

        return new AwsS3Util(
                awsS3Properties.getRegion(),
                awsS3Properties.getAccessKeyId(),
                awsS3Properties.getSecretAccessKey(),
                awsS3Properties.getBucketName()
        );
    }
}
