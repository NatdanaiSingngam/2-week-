/*
 * ==== Workmatch · Backend · เข้าถึงฐานข้อมูล (Repository) ====
 * ไฟล์: backend/src/main/java/com/workmatch/repository/ResumeSkillRepository.java
 * หน้าที่: ค้นหา / บันทึก / ลบข้อมูลในตาราง resume_skills (Spring Data สร้างคำสั่ง SQL จากชื่อเมธอดให้เอง)
 * อ่าน/เขียนตาราง: resume_skills (entity ResumeSkill) ผ่าน Spring Data JPA
 * ถูกเรียกโดย: ApplicationService, MatchService, ResumeExtrasService, ResumeService
 */
package com.workmatch.repository;

import com.workmatch.entity.ResumeSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface ResumeSkillRepository extends JpaRepository<ResumeSkill, Long> {
    List<ResumeSkill> findByResumeId(Long resumeId);

    // ใช้ตอนบันทึกเรซูเม่ใหม่: ลบทักษะชุดเดิมก่อนเพิ่มชุดใหม่
    @Transactional
    void deleteByResumeId(Long resumeId);
}
