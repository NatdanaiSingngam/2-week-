/*
 * ==== Workmatch · Backend · ตารางฐานข้อมูล (Entity) ====
 * ไฟล์: backend/src/main/java/com/workmatch/entity/VerificationStatus.java
 * หน้าที่: สถานะยืนยันตัวตนบริษัท: รอตรวจสอบ / อนุมัติ / ไม่อนุมัติ
 * ถูกเรียกโดย: AdminService, CompanyProfile, CompanyProfileRepository, DemoDataLoader, ProfileService,
 *     RegistrationService, Views
 */
package com.workmatch.entity;

/** สถานะยืนยันตัวตนสถานประกอบการ (เอกสาร 1.4.2.2, 1.4.3.5) */
public enum VerificationStatus {
    NONE("ยังไม่ส่งเอกสาร"),
    PENDING("รออนุมัติ"),
    APPROVED("ยืนยันตัวตนแล้ว"),
    REJECTED("ไม่อนุมัติ");

    private final String label;

    VerificationStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
