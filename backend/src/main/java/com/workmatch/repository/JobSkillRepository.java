/*
 * ==== Workmatch · Backend · เข้าถึงฐานข้อมูล (Repository) ====
 * ไฟล์: backend/src/main/java/com/workmatch/repository/JobSkillRepository.java
 * หน้าที่: ค้นหา / บันทึก / ลบข้อมูลในตาราง job_skills (Spring Data สร้างคำสั่ง SQL จากชื่อเมธอดให้เอง)
 * อ่าน/เขียนตาราง: job_skills (entity JobSkill) ผ่าน Spring Data JPA
 * ถูกเรียกโดย: ApplicationService, JobPostingService, JobService, MatchService
 */
package com.workmatch.repository;

import com.workmatch.entity.JobSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface JobSkillRepository extends JpaRepository<JobSkill, Long> {
    List<JobSkill> findByJobId(Long jobId);

    long countByJobId(Long jobId);

    @Transactional
    void deleteByJobId(Long jobId);
}
