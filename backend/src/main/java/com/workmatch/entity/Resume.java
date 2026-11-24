/*
 * ==== Workmatch · Backend · ตารางฐานข้อมูล (Entity) ====
 * ไฟล์: backend/src/main/java/com/workmatch/entity/Resume.java
 * หน้าที่: เรซูเม่ของนักศึกษา (คนละ 1 ฉบับ)
 * ตารางในฐานข้อมูล: resumes
 * ถูกเรียกโดย: ApplicationService, DemoDataLoader, Match, MatchService, ResumeController,
 *     ResumeExtrasService, ResumeRepository, ResumeService, ResumeSkill
 */
package com.workmatch.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** ตาราง resumes: นักศึกษา 1 คนมีเรซูเม่ 1 ฉบับ (user_id ไม่ซ้ำ) */
@Entity
@Table(name = "resumes")
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "raw_text", nullable = false, columnDefinition = "TEXT")
    private String rawText;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ไฟล์ PDF ที่อัปโหลด (ตาราง stored_files) null = กรอกเป็นข้อความอย่างเดียว
    @Column(name = "resume_file_id")
    private Long resumeFileId;

    /**
     * ทักษะที่นักศึกษากดลบเอง คั่นด้วย | (แผน 30 พ.ย.) ตอนสกัดใหม่ทั้งหมดหลังแก้พจนานุกรม NLP อาจเจอคำเดิมอีก
     * จึงต้องจำไว้ว่านักศึกษาไม่ต้องการ ล้างทิ้งเมื่อนักศึกษาบันทึกข้อความเรซูเม่ใหม่
     */
    @Column(name = "removed_skills", columnDefinition = "TEXT")
    private String removedSkills;

    public java.util.Set<String> getRemovedSkillSet() {
        java.util.Set<String> out = new java.util.LinkedHashSet<>();
        if (removedSkills != null) {
            for (String s : removedSkills.split("\\|")) if (!s.isBlank()) out.add(s.trim());
        }
        return out;
    }

    public void setRemovedSkillSet(java.util.Set<String> names) {
        removedSkills = names == null || names.isEmpty() ? null : String.join("|", names);
    }

    public Long getResumeFileId() { return resumeFileId; }
    public void setResumeFileId(Long resumeFileId) { this.resumeFileId = resumeFileId; }

    // ตั้งเวลาอัตโนมัติทั้งตอนสร้างและตอนแก้ไข (เรียกเองได้ เผื่อข้อความเหมือนเดิมแต่อยากอัปเดตเวลา)
    @PrePersist
    @PreUpdate
    public void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
