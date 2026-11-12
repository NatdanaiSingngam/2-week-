/*
 * ==== Workmatch · Backend · ข้อมูลที่ส่งให้หน้าเว็บ (DTO) ====
 * ไฟล์: backend/src/main/java/com/workmatch/dto/SkillView.java
 * หน้าที่: ทักษะ 1 ตัว (ชื่อ + หมวด) ที่ส่งให้หน้าเว็บ
 * ถูกเรียกโดย: JobDetail, JobPostingService, JobService, ResumeService, Views
 */
package com.workmatch.dto;

/** ทักษะที่ส่งให้หน้าเว็บ: {name, category} */
public class SkillView {
    private final String name;
    private final String category;

    public SkillView(String name, String category) {
        this.name = name;
        this.category = category;
    }

    public String getName() { return name; }
    public String getCategory() { return category; }
}
