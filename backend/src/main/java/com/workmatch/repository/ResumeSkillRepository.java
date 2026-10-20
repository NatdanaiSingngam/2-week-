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
