/*
 * ==== Workmatch · Backend · Controller (รับคำขอจากเบราว์เซอร์) ====
 * ไฟล์: backend/src/main/java/com/workmatch/controller/HomeController.java
 * หน้าที่: หน้าแรก /: ยังไม่ล็อกอินแสดง landing ล็อกอินแล้วพาไปหน้าหลักตามบทบาท
 * URL ที่รับ: GET /
 * แสดงผลที่หน้า: frontend/templates/landing.html
 * ถูกเรียกโดย: SecurityConfig
 */
package com.workmatch.controller;

import com.workmatch.security.RoleRedirectSuccessHandler;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // ยังไม่ login: แสดงหน้าแรก (Landing Page) ตามเอกสารออกแบบ รูปที่ 32
    // login แล้ว: ส่งไปหน้าแรกของบทบาทตัวเอง
    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) return "landing";
        return "redirect:" + RoleRedirectSuccessHandler.targetFor(authentication);
    }
}
