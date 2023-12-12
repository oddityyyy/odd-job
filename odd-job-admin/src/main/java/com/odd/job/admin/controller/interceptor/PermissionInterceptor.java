package com.odd.job.admin.controller.interceptor;

import com.odd.job.admin.controller.annotation.PermissionLimit;
import com.odd.job.admin.core.model.OddJobUser;
import com.odd.job.admin.core.util.I18nUtil;
import com.odd.job.admin.service.LoginService;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 权限拦截
 *
 * @author oddity
 * @create 2023-12-11 22:32
 */
@Component
public class PermissionInterceptor implements AsyncHandlerInterceptor {

    @Resource
    private LoginService loginService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod)) {
            return true;	// proceed with the next interceptor
        }

        // if need login
        boolean needLogin = true;
        boolean needAdminuser = false;
        HandlerMethod method = (HandlerMethod)handler;
        PermissionLimit permission = method.getMethodAnnotation(PermissionLimit.class);
        if (permission!=null) {
            needLogin = permission.limit();
            needAdminuser = permission.adminuser();
        }

        if (needLogin) {
            OddJobUser loginUser = loginService.ifLogin(request, response);
            if (loginUser == null) {
                response.setStatus(302);
                response.setHeader("location", request.getContextPath()+"/toLogin");
                return false;
            }
            if (needAdminuser && loginUser.getRole()!=1) {
                throw new RuntimeException(I18nUtil.getString("system_permission_limit"));
            }
            request.setAttribute(LoginService.LOGIN_IDENTITY_KEY, loginUser);
        }

        return true;	// proceed with the next interceptor
    }
}
