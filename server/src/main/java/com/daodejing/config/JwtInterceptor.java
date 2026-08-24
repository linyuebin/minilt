package com.daodejing.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 软解析 token：有则填充身份，无则放行（是否要求登录由业务层 AuthContext.userId() 决定）
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            Claims claims = jwtUtil.parse(auth.substring(7));
            if (claims != null) {
                long id = Long.parseLong(claims.getSubject());
                String role = claims.get("role", String.class);
                if (JwtUtil.ROLE_USER.equals(role)) {
                    AuthContext.setUserId(id);
                } else if (JwtUtil.ROLE_ADMIN.equals(role)) {
                    AuthContext.setAdminId(id);
                }
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }
}
