package com.localfresh.config;

import com.localfresh.interceptor.JwtTokenAdminInterceptor;
import com.localfresh.interceptor.JwtTokenUserInterceptor;
import com.localfresh.json.JacksonObjectMapper;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.util.List;

/**
 * 設定類，註冊web层相關元件
 */
@Configuration
@Slf4j
public class WebMvcConfiguration extends WebMvcConfigurationSupport {

    @Autowired
    private JwtTokenAdminInterceptor jwtTokenAdminInterceptor;

    @Autowired
    private JwtTokenUserInterceptor jwtTokenUserInterceptor;

    @Value("${localfresh.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}")
    private String[] allowedOriginPatterns;

    @Value("${springdoc.swagger-ui.enabled:true}")
    private boolean swaggerUiEnabled;

    /**
     * 註冊自定义拦截器
     *
     * @param registry
     */

    protected void addInterceptors(InterceptorRegistry registry) {
        log.info("开始註冊自定义拦截器...");
        registry.addInterceptor(jwtTokenAdminInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/employee/login");


        registry.addInterceptor(jwtTokenUserInterceptor)
                .addPathPatterns("/user/**")
                .excludePathPatterns("/user/member/login")
                .excludePathPatterns("/user/member/oauth/google")
                .excludePathPatterns("/user/shop/status");
    }

    @Override
    protected void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOriginPatterns)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("token", "authentication")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 透過 springdoc 產生介面文件。
     * @return
     */
    @Bean
    @ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true", matchIfMissing = true)
    public OpenAPI openAPI() {
        log.info("準備產生介面文件...");
        return new OpenAPI()
                .info(new Info()
                        .title("菜籃日 API 文件")
                        .version("3.0")
                        .description("在地小農生鮮配送平台 API 文件"));
    }

    @Bean
    @ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true", matchIfMissing = true)
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("管理端接口")
                .packagesToScan("com.localfresh.controller.admin")
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true", matchIfMissing = true)
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("用戶端接口")
                .packagesToScan("com.localfresh.controller.user")
                .build();
    }

    /**
     * 設定靜態資源映射
     * @param registry
     */
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("開始設定靜態資源映射...");
        if (swaggerUiEnabled) {
            registry.addResourceHandler("/swagger-ui/**").addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/");
            registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
        }
    }

    /**
     * 擴展 Spring MVC 訊息轉換器
     * @param converters
     */
    @Override
    protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("擴展訊息轉換器...");
        // 建立一個訊息轉換器物件
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        // 需要為訊息轉換器設定一個物件轉換器，底層使用 Jackson 將 Java 物件轉換為 JSON
        converter.setObjectMapper(new JacksonObjectMapper());
        // 將上面的訊息轉換器追加到 Spring MVC 的訊息轉換器集合中
        converters.add(0,converter);
    }
}
