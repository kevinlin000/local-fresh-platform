package com.sky.config;

import com.sky.interceptor.JwtTokenAdminInterceptor;
import com.sky.interceptor.JwtTokenUserInterceptor;
import com.sky.json.JacksonObjectMapper;
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
 * 配置类，注册web层相关组件
 */
@Configuration
@Slf4j
public class WebMvcConfiguration extends WebMvcConfigurationSupport {

    @Autowired
    private JwtTokenAdminInterceptor jwtTokenAdminInterceptor;

    @Autowired
    private JwtTokenUserInterceptor jwtTokenUserInterceptor;

    @Value("${sky.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}")
    private String[] allowedOriginPatterns;

    @Value("${springdoc.swagger-ui.enabled:true}")
    private boolean swaggerUiEnabled;

    /**
     * 注册自定义拦截器
     *
     * @param registry
     */

    protected void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器...");
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
     * 通过 springdoc 生成接口文档
     * @return
     */
    @Bean
    @ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true", matchIfMissing = true)
    public OpenAPI openAPI() {
        log.info("準備生成接口文檔...");
        return new OpenAPI()
                .info(new Info()
                        .title("蒼穹外賣項目接口文檔")
                        .version("3.0")
                        .description("蒼穹外賣項目接口文檔"));
    }

    @Bean
    @ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true", matchIfMissing = true)
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("管理端接口")
                .packagesToScan("com.sky.controller.admin")
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true", matchIfMissing = true)
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("用戶端接口")
                .packagesToScan("com.sky.controller.user")
                .build();
    }

    /**
     * 设置静态资源映射
     * @param registry
     */
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("开始设置静态资源映射...");
        if (swaggerUiEnabled) {
            registry.addResourceHandler("/swagger-ui/**").addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/");
            registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
        }
    }

    /**
     * 扩展Spring MVC消息转换器
     * @param converters
     */
    @Override
    protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("擴展消息轉換器...");
        //創建一個消息轉換器對象
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        //需要為消息轉換器設置一個對象轉換器，底層使用Jackson將Java對象轉換為JSON
        converter.setObjectMapper(new JacksonObjectMapper());
        //將上面的消息轉換器追加到Spring MVC的消息轉換器集合中
        converters.add(0,converter);
    }
}
