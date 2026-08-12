package com.example.gitprocessor.config;

import com.example.gitprocessor.controller.LoginController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Redirects unauthenticated requests to /login.
 * Static resources and the login endpoints themselves are excluded.
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String path = request.getRequestURI();

        // Always allow static resources and the login page itself
        if (path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.startsWith("/webjars/")
                || path.equals("/login")
                || path.equals("/favicon.ico")) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(LoginController.SESSION_USER_KEY) != null) {
            return true;
        }

        response.sendRedirect(request.getContextPath() + "/login");
        return false;
    }
}