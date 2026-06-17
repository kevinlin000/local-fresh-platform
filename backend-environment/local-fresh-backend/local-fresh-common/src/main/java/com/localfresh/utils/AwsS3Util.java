package com.localfresh.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Data
@AllArgsConstructor
@Slf4j
public class AwsS3Util {

    private String region;
    private String accessKeyId;
    private String secretAccessKey;
    private String bucketName;

    /**
     * 檔案上傳
     *
     * @param bytes 檔案的位元組陣列
     * @param objectName 儲存在 S3 的檔名
     * @return 圖片的外部存取網址
     */
    public String upload(byte[] bytes, String objectName) {

        // 1. 建立 AWS 憑證
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);

        // 2. 建立 S3 Client 客戶端
        S3Client s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();

        try {
            // 3. 建立上傳請求
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectName)
                    .build();

            // 4. 執行上傳
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes));

        } catch (S3Exception e) {
            log.error("上傳檔案到 AWS S3 失敗: {}", e.awsErrorDetails().errorMessage());
            throw new RuntimeException("AWS S3 上傳失敗", e);
        } finally {
            // 確保資源被關閉
            s3Client.close();
        }

        // 5. 組合 AWS S3 檔案存取路徑
        // AWS 網址規則：https://{bucketName}.s3.{region}.amazonaws.com/{objectName}
        StringBuilder stringBuilder = new StringBuilder("https://");
        stringBuilder
                .append(bucketName)
                .append(".s3.")
                .append(region)
                .append(".amazonaws.com/")
                .append(objectName);

        log.info("文件上傳到: {}", stringBuilder.toString());

        return stringBuilder.toString();
    }
}