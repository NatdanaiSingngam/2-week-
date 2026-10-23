package com.workmatch.service;

import com.workmatch.entity.Role;
import com.workmatch.entity.User;
import com.workmatch.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.regex.Pattern;

/** สมัครสมาชิก: ตรวจข้อมูล แล้วบันทึกโดยเข้ารหัสรหัสผ่านด้วย BCrypt */
@Service
public class RegistrationService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    /** ข้อมูลไม่ผ่านจะโยน IllegalArgumentException พร้อมข้อความภาษาไทยสำหรับแสดงบนหน้าเว็บ */
    @Transactional
    public User register(String displayName, String email, String password, String role) {
        String name = displayName == null ? "" : displayName.trim();
        String mail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);

        if (name.isEmpty()) throw new IllegalArgumentException("กรุณากรอกชื่อที่แสดง");
        if (name.length() > 150) throw new IllegalArgumentException("ชื่อยาวเกิน 150 ตัวอักษร");
        if (!EMAIL.matcher(mail).matches() || mail.length() > 255) {
            throw new IllegalArgumentException("รูปแบบอีเมลไม่ถูกต้อง");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("รหัสผ่านต้องยาวอย่างน้อย 8 ตัวอักษร");
        }
        Role parsedRole;
        try {
            parsedRole = Role.valueOf(role == null ? "" : role);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("กรุณาเลือกบทบาทนักศึกษาหรือบริษัท");
        }
        if (users.existsByEmail(mail)) throw new IllegalArgumentException("อีเมลนี้ถูกใช้แล้ว");

        User user = new User();
        user.setDisplayName(name);
        user.setEmail(mail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(parsedRole);
        return users.save(user);
    }
}
