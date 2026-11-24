/*
 * ==== Workmatch · Backend · Repository ====
 * ไฟล์: backend/src/main/java/com/workmatch/repository/JobScreeningCriteriaRepository.java
 * หน้าที่: อ่าน/เขียนตาราง job_screening_criteria
 * ถูกเรียกโดย: ScreeningCriteriaService, JobPostingService
 */
package com.workmatch.repository;

import com.workmatch.entity.JobScreeningCriteria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface JobScreeningCriteriaRepository extends JpaRepository<JobScreeningCriteria, Long> {
    Optional<JobScreeningCriteria> findByJobId(Long jobId);

    @Transactional
    void deleteByJobId(Long jobId);
}
