/*
 * ==== Workmatch · Backend · ตารางฐานข้อมูล (Entity) ====
 * ไฟล์: backend/src/main/java/com/workmatch/entity/ApplicationStatus.java
 * หน้าที่: สถานะใบสมัคร: pending รอพิจารณา / reviewed ตรวจสอบแล้ว / accepted ผ่าน / rejected ไม่ผ่าน พร้อมชื่อภาษาไทย
 * ถูกเรียกโดย: AdminService, ApplicationService, CompanyController, DemoDataLoader, JobApplication,
 *     JobApplicationRepository, JobController, JobPostingService, Views
 */
package com.workmatch.entity;

/**
 * สถานะการสมัครงานตาม Data Dictionary ตาราง JOB_APPLICATION (pending, reviewed, accepted, rejected)
 * และหัวข้อ 1.3.3 "ผ่าน/ไม่ผ่านการพิจารณาเบื้องต้น" ชื่อเดิมก่อนเวอร์ชัน 2: INTERVIEW -> REVIEWED, HIRED -> ACCEPTED
 * (ApplicationStatusMigration แปลงข้อมูลเดิมในฐานข้อมูลให้ตอนเปิดระบบ)
 */
public enum ApplicationStatus {
    PENDING("รอพิจารณา"),
    REVIEWED("ตรวจสอบแล้ว"),
    ACCEPTED("ผ่านการพิจารณา"),
    REJECTED("ไม่ผ่านการพิจารณา");

    private final String label;

    ApplicationStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
