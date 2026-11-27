/*
 * ==== Workmatch · Backend · ตารางฐานข้อมูล (Entity) ====
 * ไฟล์: backend/src/main/java/com/workmatch/entity/JobScreeningCriteria.java
 * หน้าที่: เกณฑ์คัดกรองผู้สมัครของประกาศงาน 1 ชุดต่อ 1 ประกาศ (A04 ตาราง 52 JOB_SCREENING_CRITERIA, UC-32)
 * ตารางในฐานข้อมูล: job_screening_criteria
 * ถูกเรียกโดย: JobScreeningCriteriaRepository, ScreeningCriteriaService
 */
package com.workmatch.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;

/**
 * ทุกคอลัมน์เกณฑ์เป็น NULL ได้ = ไม่ใช้เกณฑ์ข้อนั้น (เว้นว่างได้ทุกข้อ)
 * เกณฑ์ข้อที่ 5 "Required Skills" ไม่ได้อยู่ในตารางนี้ แต่เก็บเป็น job_skills.is_required (ตาราง 47)
 */
@Entity
@Table(name = "job_screening_criteria",
        uniqueConstraints = @UniqueConstraint(name = "uk_screening_job", columnNames = "job_id"))
public class JobScreeningCriteria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "criteria_id")
    private Long id;

    // ลบประกาศ = เกณฑ์ถูกลบตาม (ON DELETE CASCADE)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    /** คะแนนความสอดคล้องขั้นต่ำ 0–100 */
    @Column(name = "min_match_score", precision = 5, scale = 2)
    private BigDecimal minMatchScore;

    /** GPA ขั้นต่ำ 0.00–4.00 */
    @Column(name = "min_gpa", precision = 3, scale = 2)
    private BigDecimal minGpa;

    /**
     * สาขาที่รับ เลือกได้หลายสาขาจากรายการเดียวกับ StudentProfile.major (Majors) เก็บคั่นด้วย "|"
     * เช่น "วิทยาการคอมพิวเตอร์|วิศวกรรมซอฟต์แวร์"  NULL = ทุกสาขา
     */
    @Column(name = "required_major", length = 1000)
    private String requiredMajor;

    public Long getId() { return id; }
    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }
    public BigDecimal getMinMatchScore() { return minMatchScore; }
    public void setMinMatchScore(BigDecimal minMatchScore) { this.minMatchScore = minMatchScore; }
    public BigDecimal getMinGpa() { return minGpa; }
    public void setMinGpa(BigDecimal minGpa) { this.minGpa = minGpa; }
    public String getRequiredMajor() { return requiredMajor; }
    public void setRequiredMajor(String requiredMajor) { this.requiredMajor = requiredMajor; }

    /** สาขาที่รับเป็นรายการ (ว่าง = ทุกสาขา) */
    public java.util.List<String> getRequiredMajors() {
        if (requiredMajor == null || requiredMajor.isBlank()) return java.util.List.of();
        return java.util.Arrays.stream(requiredMajor.split("\\|")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public void setRequiredMajors(java.util.List<String> majors) {
        this.requiredMajor = majors == null || majors.isEmpty() ? null : String.join("|", majors);
    }

    /** ยังไม่ได้ตั้งเกณฑ์ข้อใดเลย */
    public boolean isEmpty() {
        return minMatchScore == null && minGpa == null && requiredMajor == null;
    }
}
