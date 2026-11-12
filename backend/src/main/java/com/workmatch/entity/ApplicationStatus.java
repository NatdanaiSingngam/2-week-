/*
 * ==== Workmatch · Backend · ตารางฐานข้อมูล (Entity) ====
 * ไฟล์: backend/src/main/java/com/workmatch/entity/ApplicationStatus.java
 * หน้าที่: สถานะใบสมัคร: รอพิจารณา / นัดสัมภาษณ์ / ผ่าน / ไม่ผ่าน พร้อมชื่อภาษาไทย
 * ถูกเรียกโดย: AdminService, ApplicationService, CompanyController, DemoDataLoader, JobApplication,
 *     JobApplicationRepository, JobController, JobPostingService, Views
 */
package com.workmatch.entity;

/** สถานะการสมัครงาน (เอกสาร รูปที่ 42, 45, 46): รอพิจารณา -> นัดสัมภาษณ์ -> รับเข้าทำงาน / ปฏิเสธ */
public enum ApplicationStatus {
    PENDING("รอพิจารณา"),
    INTERVIEW("นัดสัมภาษณ์"),
    HIRED("รับเข้าทำงาน"),
    REJECTED("ปฏิเสธ");

    private final String label;

    ApplicationStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
