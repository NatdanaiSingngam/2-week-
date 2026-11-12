/*
 * ==== Workmatch · Backend · เข้าถึงฐานข้อมูล (Repository) ====
 * ไฟล์: backend/src/main/java/com/workmatch/repository/StudentProfileRepository.java
 * หน้าที่: ค้นหา / บันทึก / ลบข้อมูลในตาราง student_profiles (Spring Data สร้างคำสั่ง SQL จากชื่อเมธอดให้เอง)
 * อ่าน/เขียนตาราง: student_profiles (entity StudentProfile) ผ่าน Spring Data JPA
 * ถูกเรียกโดย: ApplicationService, DemoDataLoader, ProfileService, RegistrationService
 */
package com.workmatch.repository;

import com.workmatch.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {
    Optional<StudentProfile> findByUserId(Long userId);
    boolean existsByStudentId(String studentId);
}
