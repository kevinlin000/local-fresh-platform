package com.localfresh.interceptor;

import com.localfresh.constant.JwtClaimsConstant;
import com.localfresh.constant.EmployeeRoleConstant;
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
 * JWT 令牌驗證攔截器
 */
@Component
@Slf4j
public class JwtTokenAdminInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 驗證 JWT
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 判斷目前攔截到的是 Controller 方法還是其他資源
        if (!(handler instanceof HandlerMethod)) {
            // 目前攔截到的不是動態方法，直接放行
            return true;
        }

        // 1、從請求標頭中取得令牌
        String token = request.getHeader(jwtProperties.getAdminTokenName());

        // 2、驗證令牌
        try {
            log.info("JWT 驗證");
            Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
            Long empId = Long.valueOf(claims.get(JwtClaimsConstant.EMP_ID).toString());
            String role = claims.get(JwtClaimsConstant.ROLE, String.class);
            if (role == null || role.isBlank()) {
                role = EmployeeRoleConstant.ADMIN;
            }
            log.info("目前員工 id：{}", empId);
            BaseContext.setCurrentId(empId);
            BaseContext.setCurrentRole(role);
            // 3、通過驗證，放行
            return true;
        } catch (Exception ex) {
            // 4、未通過驗證，回應 401 狀態碼
            response.setStatus(401);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, @Nullable Exception ex) {
        BaseContext.removeCurrentId();
        BaseContext.removeCurrentRole();
    }
}
