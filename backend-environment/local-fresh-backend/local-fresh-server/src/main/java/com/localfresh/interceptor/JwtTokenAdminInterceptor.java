package com.localfresh.interceptor;

import com.localfresh.constant.JwtClaimsConstant;
import com.localfresh.context.BaseContext;
import com.localfresh.properties.JwtProperties;
import com.localfresh.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.Nullable;

/**
 * jwt令牌校验的拦截器
 */
@Component
@Slf4j
public class JwtTokenAdminInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 校验jwt
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //判断目前拦截到的是Controller的方法还是其他資源
        if (!(handler instanceof HandlerMethod)) {
            //目前拦截到的不是动态方法，直接放行
            return true;
        }

        //1、從請求头中取得令牌
        String token = request.getHeader(jwtProperties.getAdminTokenName());

        //2、校验令牌
        try {
            log.info("jwt校验");
            Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
            Long empId = Long.valueOf(claims.get(JwtClaimsConstant.EMP_ID).toString());
            log.info("目前员工id：{}", empId);
            BaseContext.setCurrentId(empId);
            //3、透過，放行
            return true;
        } catch (Exception ex) {
            //4、不透過，响应401狀態码
            response.setStatus(401);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, @Nullable Exception ex) {
        BaseContext.removeCurrentId();
    }
}
