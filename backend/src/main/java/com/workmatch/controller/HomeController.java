package com.workmatch.controller;

import com.workmatch.security.RoleRedirectSuccessHandler;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // ยังไม่ login: Spring Security ส่งไป /login ก่อนถึงตรงนี้
    // login แล้ว: ส่งไปหน้าแรกของบทบาทตัวเอง
    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication == null) return "redirect:/login";
        return "redirect:" + RoleRedirectSuccessHandler.targetFor(authentication);
    }
}
