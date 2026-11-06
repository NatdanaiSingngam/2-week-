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
     * ลงทะเบียนตามเอกสาร รูปที่ 34 (นักศึกษา: ชื่อ นามสกุล รหัสนิสิต) และรูปที่ 35 (สถานประกอบการ: ผู้ติดต่อ ชื่อบริษัท เลขผู้เสียภาษี)
     * ถ้าส่งแค่ displayName แบบเดิม (ไม่มี studentId / taxId) ยังสมัครได้เหมือนเดิม
     * ไม่ผ่านให้คืนหน้า register ใน request เดิม (ไม่ redirect) หน้าเว็บเติมค่ากลับจาก param เอง ส่วนรหัสผ่านไม่เติมกลับ
     */
    @PostMapping("/register")
    public String register(@RequestParam(required = false) String displayName,
                           @RequestParam(required = false) String email,
                           @RequestParam(required = false) String password,
                           @RequestParam(required = false) String role,
                           @RequestParam(required = false) String firstName,
                           @RequestParam(required = false) String lastName,
                           @RequestParam(required = false) String studentId,
                           @RequestParam(required = false) String companyName,
                           @RequestParam(required = false) String taxId,
                           Model model) {
        try {
            if ("STUDENT".equals(role) && studentId != null) {
                registrationService.registerStudent(firstName, lastName, studentId, email, password);
            } else if ("COMPANY".equals(role) && taxId != null) {
                registrationService.registerCompany(firstName, lastName, companyName, taxId, email, password);
            } else {
                registrationService.register(displayName, email, password, role);
            }
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
        return "redirect:/login?registered";
    }
}
