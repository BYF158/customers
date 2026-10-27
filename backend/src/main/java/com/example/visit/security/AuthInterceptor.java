package com.example.visit.security;

import com.example.visit.common.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw BizException.unauthorized("请先登录");
        }
        CurrentUser user = jwtUtil.parse(header.substring(7).trim());
        UserContext.set(user);

        boolean adminOnly = method.hasMethodAnnotation(AdminOnly.class)
                || method.getBeanType().isAnnotationPresent(AdminOnly.class);
        if (adminOnly && !user.isAdmin()) {
            throw BizException.forbidden("该操作仅管理者可用");
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
