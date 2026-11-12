/*
 * ==== Workmatch · Backend · ข้อมูลที่ส่งให้หน้าเว็บ (DTO) ====
 * ไฟล์: backend/src/main/java/com/workmatch/dto/JobForm.java
 * หน้าที่: ค่าที่กรอกในฟอร์มประกาศงานแบบเดิม (ชื่อ + รายละเอียด)
 */
package com.workmatch.dto;

/** ค่าที่กรอกในฟอร์ม company/job-form: {title, description} */
public class JobForm {
    private final String title;
    private final String description;

    public JobForm(String title, String description) {
        this.title = title == null ? "" : title;
        this.description = description == null ? "" : description;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
}
