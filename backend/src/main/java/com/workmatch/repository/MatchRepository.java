/*
 * ==== Workmatch · Backend · เข้าถึงฐานข้อมูล (Repository) ====
 * ไฟล์: backend/src/main/java/com/workmatch/repository/MatchRepository.java
 * หน้าที่: ค้นหา / บันทึก / ลบข้อมูลในตาราง matches (Spring Data สร้างคำสั่ง SQL จากชื่อเมธอดให้เอง)
 * อ่าน/เขียนตาราง: matches (entity Match) ผ่าน Spring Data JPA
 * ถูกเรียกโดย: AdminService, JobPostingService, JobService, MatchService
 */
package com.workmatch.repository;

import com.workmatch.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByResumeId(Long resumeId);
    List<Match> findByJobId(Long jobId);
    Optional<Match> findByResumeIdAndJobId(Long resumeId, Long jobId);

    // จำนวนผู้ที่จับคู่ได้คะแนนมากกว่า 0 (ใช้เป็น applicantCount)
    long countByJobIdAndScoreGreaterThan(Long jobId, java.math.BigDecimal score);

    // ลบผลจับคู่ของประกาศ (ก่อนลบประกาศงาน)
    @Transactional
    void deleteByJobId(Long jobId);

    @Query("select avg(m.score) from Match m")
    Double averageScore();
}
