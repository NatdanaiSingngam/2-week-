/*
 * ==== Workmatch · Backend · เข้าถึงฐานข้อมูล (Repository) ====
 * ไฟล์: backend/src/main/java/com/workmatch/repository/CompanyProfileRepository.java
 * หน้าที่: ค้นหา / บันทึก / ลบข้อมูลในตาราง company_profiles (Spring Data สร้างคำสั่ง SQL จากชื่อเมธอดให้เอง)
 * อ่าน/เขียนตาราง: company_profiles (entity CompanyProfile) ผ่าน Spring Data JPA
 * ถูกเรียกโดย: AdminService, DemoDataLoader, GlobalModelAdvice, JobPostingService, ProfileService,
 *     RegistrationService
 */
package com.workmatch.repository;

import com.workmatch.entity.CompanyProfile;
import com.workmatch.entity.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CompanyProfileRepository extends JpaRepository<CompanyProfile, Long> {
    Optional<CompanyProfile> findByUserId(Long userId);
    long countByVerificationStatus(VerificationStatus status);

    // คำขอยืนยันตัวตนสำหรับผู้ดูแล: ที่ส่งเอกสารแล้ว (รออนุมัติขึ้นก่อน)
    @Query("select p from CompanyProfile p join fetch p.user where p.verificationStatus is not null "
            + "and p.verificationStatus <> com.workmatch.entity.VerificationStatus.NONE order by p.submittedAt desc")
    List<CompanyProfile> findSubmitted();

    @Query("select distinct p.businessType from CompanyProfile p where p.businessType is not null and p.businessType <> '' order by p.businessType")
    List<String> findBusinessTypes();
}
