/*
 * ==== Workmatch · Backend · Service (ตรรกะหลักของระบบ) ====
 * ไฟล์: backend/src/main/java/com/workmatch/service/RegistrationService.java
 * หน้าที่: สมัครสมาชิก: ตรวจข้อมูล เข้ารหัสรหัสผ่านด้วย BCrypt แล้วบันทึก
 * เรียกใช้: UserRepository, StudentProfileRepository, CompanyProfileRepository
 * ถูกเรียกโดย: AdminAccountInitializer, AuthController, DemoDataLoader
 */
package com.workmatch.service;

import com.workmatch.entity.CompanyProfile;
import com.workmatch.entity.Role;
import com.workmatch.entity.StudentProfile;
import com.workmatch.entity.User;
import com.workmatch.entity.VerificationStatus;
import com.workmatch.repository.CompanyProfileRepository;
import com.workmatch.repository.StudentProfileRepository;
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
    private final StudentProfileRepository studentProfiles;
    private final CompanyProfileRepository companyProfiles;

    public RegistrationService(UserRepository users, PasswordEncoder passwordEncoder,
                               StudentProfileRepository studentProfiles, CompanyProfileRepository companyProfiles) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.studentProfiles = studentProfiles;
        this.companyProfiles = companyProfiles;
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
        // ผู้ดูแลระบบสมัครผ่านหน้าเว็บไม่ได้
        if (parsedRole == Role.ADMIN) throw new IllegalArgumentException("กรุณาเลือกบทบาทนักศึกษาหรือบริษัท");
        if (users.existsByEmail(mail)) throw new IllegalArgumentException("อีเมลนี้ถูกใช้แล้ว");

        User user = new User();
        user.setDisplayName(name);
        user.setEmail(mail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(parsedRole);
        return users.save(user);
    }

    /** ลงทะเบียนนักศึกษาแบบเต็มตามเอกสาร รูปที่ 34: ชื่อจริง นามสกุล รหัสนิสิต 8 หลัก */
    @Transactional
    public User registerStudent(String firstName, String lastName, String studentId, String email, String password) {
        String first = ProfileService.trim(firstName);
        String last = ProfileService.trim(lastName);
        String sid = ProfileService.trim(studentId);
        if (first.isEmpty() || last.isEmpty()) throw new IllegalArgumentException("กรุณากรอกชื่อจริงและนามสกุล");
        if (!ProfileService.STUDENT_ID.matcher(sid).matches()) throw new IllegalArgumentException("รหัสนิสิตต้องเป็นตัวเลข 8 หลัก");
        if (studentProfiles.existsByStudentId(sid)) throw new IllegalArgumentException("รหัสนิสิตนี้ลงทะเบียนแล้ว");

        User user = register(first + " " + last, email, password, Role.STUDENT.name());
        StudentProfile p = new StudentProfile();
        p.setUser(user);
        p.setFirstName(ProfileService.limit(first, 100));
        p.setLastName(ProfileService.limit(last, 100));
        p.setStudentId(sid);
        studentProfiles.save(p);
        return user;
    }

    /** ลงทะเบียนสถานประกอบการตามเอกสาร รูปที่ 35: ชื่อผู้ติดต่อ ชื่อสถานประกอบการ เลขผู้เสียภาษี 13 หลัก */
    @Transactional
    public User registerCompany(String contactFirstName, String contactLastName, String companyName, String taxId,
                                String email, String password) {
        String first = ProfileService.trim(contactFirstName);
        String last = ProfileService.trim(contactLastName);
        String tax = ProfileService.trim(taxId);
        if (first.isEmpty() || last.isEmpty()) throw new IllegalArgumentException("กรุณากรอกชื่อและนามสกุลผู้ติดต่อ");
        if (!ProfileService.TAX_ID.matcher(tax).matches()) {
            throw new IllegalArgumentException("เลขประจำตัวผู้เสียภาษีต้องเป็นตัวเลข 13 หลัก");
        }
        User user = register(companyName, email, password, Role.COMPANY.name());
        CompanyProfile p = new CompanyProfile();
        p.setUser(user);
        p.setContactFirstName(ProfileService.limit(first, 100));
        p.setContactLastName(ProfileService.limit(last, 100));
        p.setTaxId(tax);
        p.setVerificationStatus(VerificationStatus.NONE);
        companyProfiles.save(p);
        return user;
    }

    /** สร้างบัญชีผู้ดูแลระบบ (เรียกจาก AdminAccountInitializer / ข้อมูลตัวอย่างเท่านั้น) */
    @Transactional
    public User createAdmin(String displayName, String email, String password) {
        String mail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(mail).matches()) throw new IllegalArgumentException("รูปแบบอีเมลผู้ดูแลไม่ถูกต้อง");
        if (password == null || password.length() < 8) throw new IllegalArgumentException("รหัสผ่านผู้ดูแลต้องยาวอย่างน้อย 8 ตัวอักษร");
        if (users.existsByEmail(mail)) throw new IllegalArgumentException("อีเมลนี้ถูกใช้แล้ว");
        User user = new User();
        user.setDisplayName(displayName);
        user.setEmail(mail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(Role.ADMIN);
        return users.save(user);
    }
}
