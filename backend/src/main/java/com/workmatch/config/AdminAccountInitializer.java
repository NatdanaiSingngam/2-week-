/*
 * ==== Workmatch · Backend · ตั้งค่าระบบ ====
 * ไฟล์: backend/src/main/java/com/workmatch/config/AdminAccountInitializer.java
 * หน้าที่: สร้างบัญชีผู้ดูแลระบบตอนเปิดระบบ จากตัวแปรสภาพแวดล้อม WORKMATCH_ADMIN_EMAIL / WORKMATCH_ADMIN_PASSWORD
 * เรียกใช้: UserRepository, RegistrationService
 * ถูกเรียกโดย: RegistrationService
 */
package com.workmatch.config;

import com.workmatch.entity.Role;
import com.workmatch.repository.UserRepository;
import com.workmatch.service.RegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * สร้างบัญชีผู้ดูแลระบบตอนเปิดระบบ ถ้าตั้ง WORKMATCH_ADMIN_EMAIL และ WORKMATCH_ADMIN_PASSWORD ไว้ และยังไม่มี ADMIN
 * (ผู้ดูแลระบบสมัครผ่านหน้าเว็บไม่ได้) รหัสผ่านอ่านจากตัวแปรสภาพแวดล้อมเท่านั้น ไม่เขียนลงไฟล์
 */
@Component
public class AdminAccountInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final UserRepository users;
    private final RegistrationService registrationService;
    private final String email;
    private final String password;

    public AdminAccountInitializer(UserRepository users, RegistrationService registrationService,
                                   @Value("${workmatch.admin.email:}") String email,
                                   @Value("${workmatch.admin.password:}") String password) {
        this.users = users;
        this.registrationService = registrationService;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) return;
        if (users.existsByRole(Role.ADMIN) || users.existsByEmail(email.trim().toLowerCase())) return;
        try {
            registrationService.createAdmin("ผู้ดูแลระบบ", email, password);
            log.info("สร้างบัญชีผู้ดูแลระบบ {} แล้ว", email);
        } catch (IllegalArgumentException e) {
            log.warn("สร้างบัญชีผู้ดูแลระบบไม่สำเร็จ: {}", e.getMessage());
        }
    }
}
