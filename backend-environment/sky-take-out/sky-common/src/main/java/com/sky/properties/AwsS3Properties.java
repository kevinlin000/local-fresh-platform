package com.sky.properties;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sky.aws.s3") // 這裡對應你在 application.yml 寫的層級
@Data
@ToString(exclude = {"accessKeyId", "secretAccessKey"})
public class AwsS3Properties {

    private String region;
    private String accessKeyId;
    private String secretAccessKey;
    private String bucketName;

}
