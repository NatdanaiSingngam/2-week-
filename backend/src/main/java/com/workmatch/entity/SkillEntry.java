/*
 * ==== Workmatch · Backend · ตารางฐานข้อมูล (Entity) ====
 * ไฟล์: backend/src/main/java/com/workmatch/entity/SkillEntry.java
 * หน้าที่: พจนานุกรมทักษะมาตรฐาน (ต้นฉบับของ nlp-service/nlp_service/skill_dictionary.csv)
 * ตารางในฐานข้อมูล: skill_dictionary
 * ถูกเรียกโดย: DictionaryService, JobPostingService, ResumeExtrasService, SkillEntryRepository, SkillSynonym
 */
package com.workmatch.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * ตาราง skill_dictionary: พจนานุกรมทักษะมาตรฐาน (เอกสาร 1.4.3.2, รูปที่ 50)
 * เป็นต้นฉบับของ nlp-service/nlp_service/skill_dictionary.csv: ผู้ดูแลแก้ที่นี่แล้วระบบส่งชุดใหม่ให้ NLP (DictionaryService)
 * active = false คือ "ปิดใช้งาน" NLP จะไม่สกัดคำนี้ และไม่แสดงเป็นตัวเลือกในแบบฟอร์ม
 */
@Entity
@Table(name = "skill_dictionary")
public class SkillEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
