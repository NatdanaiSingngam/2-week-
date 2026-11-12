/*
 * ==== Workmatch · Backend · เข้าถึงฐานข้อมูล (Repository) ====
 * ไฟล์: backend/src/main/java/com/workmatch/repository/ResumeRepository.java
 * หน้าที่: ค้นหา / บันทึก / ลบข้อมูลในตาราง resumes (Spring Data สร้างคำสั่ง SQL จากชื่อเมธอดให้เอง)
 * อ่าน/เขียนตาราง: resumes (entity Resume) ผ่าน Spring Data JPA
 * ถูกเรียกโดย: ApplicationService, JobPostingService, MatchService, ResumeExtrasService, ResumeService
 */
package com.workmatch.repository;

import com.workmatch.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {
    // นักศึกษา 1 คนมีเรซูเม่ได้ 1 ฉบับ
    Optional<Resume> findByUserId(Long userId);
}
