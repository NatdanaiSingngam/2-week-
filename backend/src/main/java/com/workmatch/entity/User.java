/*
 * ==== Workmatch · Backend · ตารางฐานข้อมูล (Entity) ====
 * ไฟล์: backend/src/main/java/com/workmatch/entity/User.java
 * หน้าที่: ผู้ใช้ทุกบทบาท (อีเมล รหัสผ่านที่เข้ารหัสแล้ว ชื่อที่แสดง บทบาท)
 * ตารางในฐานข้อมูล: users
 * ถูกเรียกโดย: AdminService, ApplicationService, Bookmark, BookmarkService, CompanyController,
 *     CompanyProfile, CurrentUserService, CustomUserDetailsService, DemoDataLoader, FileStorageService, Job,
 *     JobApplication, JobController, JobPostingService, JobService, MatchService, ProfileService,
 *     RegistrationService, Resume, ResumeController, ResumeExtrasService, ResumeService, StoredFile,
 *     StudentController, StudentDashboardController, StudentProfile, UserRepository
 */
package com.workmatch.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** ตาราง users: ผู้ใช้ทั้งนักศึกษาและบริษัท */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    // เก็บเฉพาะค่า BCrypt ห้ามเก็บรหัสผ่านจริง
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ผู้ดูแลระบบระงับ/เปิดใช้งานบัญชีได้ (เอกสาร 1.4.3.1.3) null = ใช้งานได้ (ข้อมูลเดิมก่อนมีคอลัมน์นี้)
    @Column(name = "enabled")
    private Boolean enabled;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (enabled == null) enabled = Boolean.TRUE;
    }

    public boolean isEnabled() { return enabled == null || enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
