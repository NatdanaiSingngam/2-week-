/*
 * ==== Workmatch · Backend · ตารางฐานข้อมูล (Entity) ====
 * ไฟล์: backend/src/main/java/com/workmatch/entity/CompanyProfile.java
 * หน้าที่: ข้อมูลสถานประกอบการและสถานะยืนยันตัวตน
 * ตารางในฐานข้อมูล: company_profiles
 * ถูกเรียกโดย: AdminService, CompanyProfileRepository, DemoDataLoader, JobPostingService, ProfileService,
 *     RegistrationService
 */
package com.workmatch.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * ตาราง company_profiles: ข้อมูลสถานประกอบการและการยืนยันตัวตน (เอกสาร รูปที่ 35, 43, 49)
 * ชื่อบริษัทอยู่ที่ users.display_name เอกสาร ภ.พ.20 อยู่ในตาราง stored_files (document_file_id)
 */
@Entity
@Table(name = "company_profiles")
public class CompanyProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(name = "contact_first_name", length = 100)
    private String contactFirstName;

    @Column(name = "contact_last_name", length = 100)
    private String contactLastName;

    @Column(name = "tax_id", length = 13)
    private String taxId;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "business_type", length = 150)
    private String businessType;

    @Column(name = "employee_count", length = 50)
    private String employeeCount;

    @Column(name = "selection_method", length = 200)
    private String selectionMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", length = 10)
    private VerificationStatus verificationStatus;

    @Column(name = "document_file_id")
    private Long documentFileId;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by", length = 255)
    private String reviewedBy;

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getContactFirstName() { return contactFirstName; }
    public void setContactFirstName(String v) { this.contactFirstName = v; }
    public String getContactLastName() { return contactLastName; }
    public void setContactLastName(String v) { this.contactLastName = v; }
    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public String getEmployeeCount() { return employeeCount; }
    public void setEmployeeCount(String employeeCount) { this.employeeCount = employeeCount; }
    public String getSelectionMethod() { return selectionMethod; }
    public void setSelectionMethod(String selectionMethod) { this.selectionMethod = selectionMethod; }
    public VerificationStatus getVerificationStatus() {
        return verificationStatus == null ? VerificationStatus.NONE : verificationStatus;
    }
    public void setVerificationStatus(VerificationStatus s) { this.verificationStatus = s; }
    public boolean isVerified() { return getVerificationStatus() == VerificationStatus.APPROVED; }
    public Long getDocumentFileId() { return documentFileId; }
    public void setDocumentFileId(Long documentFileId) { this.documentFileId = documentFileId; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
}
