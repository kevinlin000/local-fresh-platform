package com.localfresh.interceptor;

import com.localfresh.constant.EmployeeRoleConstant;
import com.localfresh.context.BaseContext;
import com.localfresh.exception.ForbiddenOperationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminReadOnlyRoleInterceptor implements HandlerInterceptor {

    private static final String MESSAGE = "展示帳號僅供查看，不能修改後台資料";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod) || !EmployeeRoleConstant.VIEWER.equals(BaseContext.getCurrentRole())) {
            return true;
        }
        if (isReadOnly(request) || isLogout(request)) {
            return true;
        }
        throw new ForbiddenOperationException(MESSAGE);
    }

    private boolean isReadOnly(HttpServletRequest request) {
        String method = request.getMethod();
        return HttpMethod.GET.matches(method)
                || HttpMethod.HEAD.matches(method)
                || HttpMethod.OPTIONS.matches(method);
    }

    private boolean isLogout(HttpServletRequest request) {
        return HttpMethod.POST.matches(request.getMethod())
                && "/admin/employee/logout".equals(request.getRequestURI());
    }
}
