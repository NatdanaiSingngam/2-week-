/*
 * ==== Workmatch · Backend · ตารางฐานข้อมูล (Entity) ====
 * ไฟล์: backend/src/main/java/com/workmatch/entity/StudentProfile.java
 * หน้าที่: ข้อมูลส่วนตัวนักศึกษา (รหัสนิสิต คณะ สาขา ฯลฯ)
 * ตารางในฐานข้อมูล: student_profiles
 * ถูกเรียกโดย: ApplicationService, DemoDataLoader, ProfileService, RegistrationService,
 *     StudentProfileRepository
 */
package com.workmatch.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/** ตาราง student_profiles: ข้อมูลนักศึกษา (เอกสาร รูปที่ 34, 36) 1 คน 1 แถว ลบผู้ใช้แล้วลบตามอัตโนมัติ */
@Entity
@Table(name = "student_profiles")
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "student_id", length = 8)
    private String studentId;

    @Column(length = 20)
    private String phone;

    @Column(length = 150)
    private String faculty;

    @Column(length = 150)
    private String major;

    // ข้อมูลที่เว็บหางานนักศึกษาทั่วไปเก็บ ทุกช่องไม่บังคับ; GPA แสดงให้บริษัทเห็นเมื่อนักศึกษายินยอมเท่านั้น
    @Column(precision = 3, scale = 2)
    private BigDecimal gpa;

    @Column(name = "gpa_visible")
    private Boolean gpaVisible;

    @Column(name = "coop_start")
    private LocalDate coopStart;

    @Column(name = "coop_end")
    private LocalDate coopEnd;

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getFaculty() { return faculty; }
    public void setFaculty(String faculty) { this.faculty = faculty; }
    public BigDecimal getGpa() { return gpa; }
    public void setGpa(BigDecimal gpa) { this.gpa = gpa; }
    public boolean isGpaVisible() { return Boolean.TRUE.equals(gpaVisible); }
    public void setGpaVisible(boolean v) { this.gpaVisible = v; }
    public LocalDate getCoopStart() { return coopStart; }
    public void setCoopStart(LocalDate coopStart) { this.coopStart = coopStart; }
    public LocalDate getCoopEnd() { return coopEnd; }
    public void setCoopEnd(LocalDate coopEnd) { this.coopEnd = coopEnd; }
    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }
}
