package com.workmatch.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

/** หลัง login: STUDENT ไป /student/dashboard, COMPANY ไป /company/jobs */
public class RoleRedirectSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        response.sendRedirect(request.getContextPath() + targetFor(authentication));
    }

    public static String targetFor(Authentication authentication) {
        for (GrantedAuthority a : authentication.getAuthorities()) {
            if ("ROLE_STUDENT".equals(a.getAuthority())) return "/student/dashboard";
            if ("ROLE_COMPANY".equals(a.getAuthority())) return "/company/jobs";
        }
        return "/login";
    }
}
