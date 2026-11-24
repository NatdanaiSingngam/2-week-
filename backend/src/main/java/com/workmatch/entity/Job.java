/*
 * ==== Workmatch · Backend · ตารางฐานข้อมูล (Entity) ====
 * ไฟล์: backend/src/main/java/com/workmatch/entity/Job.java
 * หน้าที่: ประกาศงานของบริษัท
 * ตารางในฐานข้อมูล: jobs
 * ถูกเรียกโดย: ApplicationService, Bookmark, BookmarkService, DemoDataLoader, JobApplication,
 *     JobPostingService, JobRepository, JobService, JobSkill, Match, MatchService
 */
package com.workmatch.entity;

import java.time.LocalDate;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** ตาราง jobs: ประกาศงานของบริษัท (company_id อ้างถึง users) */
@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private User company;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ข้อมูลเพิ่มเติมตามแบบฟอร์มสร้างประกาศงาน (เอกสาร รูปที่ 44) ไม่บังคับกรอก
    @Column(length = 255)
    private String compensation;

    @Column(name = "work_schedule", length = 255)
    private String workSchedule;

    @Column(length = 255)
    private String accommodation;

    @Column(length = 255)
    private String transport;

    @Column(length = 255)
    private String location;

    @Column(name = "work_mode", length = 20)
    private String workMode;

    private Integer openings;

    // วันสุดท้ายที่รับสมัคร (รวมวันนั้น); null = ไม่กำหนด
    private LocalDate deadline;

    // OPEN = เปิดรับสมัคร, DRAFT = แบบร่าง (ยังไม่จับคู่/ไม่แสดงให้นักศึกษา), CLOSED = ปิดรับ; null (ข้อมูลเดิม) ถือว่า OPEN
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private JobStatus status;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = JobStatus.OPEN;
    }

    public String getCompensation() { return compensation; }
    public void setCompensation(String compensation) { this.compensation = compensation; }
    public String getWorkSchedule() { return workSchedule; }
    public void setWorkSchedule(String workSchedule) { this.workSchedule = workSchedule; }
    public String getAccommodation() { return accommodation; }
    public void setAccommodation(String accommodation) { this.accommodation = accommodation; }
    public String getTransport() { return transport; }
    public void setTransport(String transport) { this.transport = transport; }
    public JobStatus getStatus() { return status == null ? JobStatus.OPEN : status; }
    public void setStatus(JobStatus status) { this.status = status; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getWorkMode() { return workMode; }
    public void setWorkMode(String workMode) { this.workMode = workMode; }
    public Integer getOpenings() { return openings; }
    public void setOpenings(Integer openings) { this.openings = openings; }
    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }
    public boolean isExpired() { return deadline != null && deadline.isBefore(LocalDate.now()); }
    /** เปิดรับและยังไม่เลยวันปิดรับสมัคร */
    public boolean isAccepting() { return isOpen() && !isExpired(); }
    public boolean isOpen() { return getStatus() == JobStatus.OPEN; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getCompany() { return company; }
    public void setCompany(User company) { this.company = company; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
