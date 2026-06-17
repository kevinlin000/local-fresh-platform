package com.localfresh.test;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

// @SpringBootTest
@Disabled("跟著課程實作的 demo 測試，需要外部依賴（HTTP server / Redis），不適合自動化測試環境")
public class HttpClientTest {

    /**
     * 測試通過httpclient發送GET方式的請求
     */
    @Test
    public void testGET() throws Exception {
        //創建httpclient對象
        CloseableHttpClient httpClient = HttpClients.createDefault();
        //創建請求對象
        HttpGet httpGet = new HttpGet("http://localhost:8080/user/shop/status");
        //發送請求
        CloseableHttpResponse response = httpClient.execute(httpGet);

        //獲取服務端返回的狀態碼
        int statusCode = response.getStatusLine().getStatusCode();
        System.out.println("服務端返回的狀態碼為：" + statusCode);

        HttpEntity entity = response.getEntity();
        String body = EntityUtils.toString(entity);
        System.out.println("服務端返回的數據為：" + body);

        //關閉資源
        response.close();
        httpClient.close();


    }

    /**
     * 測試通過httpclient發送POST方式的請求
     */
    @Test
    public void testPOST() throws Exception {
        //創建httpclient對象
        CloseableHttpClient httpClient = HttpClients.createDefault();
        //創建請求對象
        HttpPost httpPost = new HttpPost("http://localhost:8080/admin/employee/login");

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("username","admin");
        jsonObject.put("password","123456");

        StringEntity entity = new StringEntity(jsonObject.toString());
        //指定請求編碼方式
        entity.setContentEncoding("UTF-8");
        //數據格式
        entity.setContentType("application/json");
        httpPost.setEntity(entity);

        //發送請求
        CloseableHttpResponse response = httpClient.execute(httpPost);

        //解析返回結果
        int statusCode = response.getStatusLine().getStatusCode();
        System.out.println("響應碼為：" + statusCode);

        HttpEntity entity1 = response.getEntity();
        String body = EntityUtils.toString(entity1);
        System.out.println("響應數據為：" + body);

        //關閉資源
        response.close();
        httpClient.close();


    }

}
