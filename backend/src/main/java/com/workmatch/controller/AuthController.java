package com.workmatch.controller;

import com.workmatch.service.RegistrationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final RegistrationService registrationService;

    public AuthController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    /** หน้า login อ่านข้อความแจ้งจาก param.error / param.logout / param.registered เอง */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm() {
        return "register";
    }

    /**
     * ถ้าไม่ผ่านให้คืนหน้า register ใน request เดิม (ไม่ redirect)
     * หน้าเว็บจะเติมชื่อ/อีเมล/บทบาทกลับจาก param เอง ส่วนรหัสผ่านไม่เติมกลับ
     */
    @PostMapping("/register")
    public String register(@RequestParam(required = false) String displayName,
                           @RequestParam(required = false) String email,
                           @RequestParam(required = false) String password,
                           @RequestParam(required = false) String role,
                           Model model) {
        try {
            registrationService.register(displayName, email, password, role);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
        return "redirect:/login?registered";
    }
}
