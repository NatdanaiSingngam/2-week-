/*
 * ==== Workmatch · Backend · เข้าถึงฐานข้อมูล (Repository) ====
 * ไฟล์: backend/src/main/java/com/workmatch/repository/JobRepository.java
 * หน้าที่: ค้นหา / บันทึก / ลบข้อมูลในตาราง jobs (Spring Data สร้างคำสั่ง SQL จากชื่อเมธอดให้เอง)
 * อ่าน/เขียนตาราง: jobs (entity Job) ผ่าน Spring Data JPA
 * ถูกเรียกโดย: AdminService, ApplicationService, BookmarkService, DemoDataLoader, JobPostingService,
 *     JobService, MatchService
 */
package com.workmatch.repository;

import com.workmatch.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByCompanyId(Long companyId);

    // ประกาศที่เปิดรับสมัคร (status เป็น null = ข้อมูลก่อนมีคอลัมน์ ถือว่าเปิดรับ) ใหม่สุดก่อน
    @Query("select j from Job j join fetch j.company where j.status is null or j.status = com.workmatch.entity.JobStatus.OPEN order by j.createdAt desc, j.id desc")
    List<Job> findOpenJobs();

    @Query("select count(j) from Job j where j.status is null or j.status = com.workmatch.entity.JobStatus.OPEN")
    long countOpenJobs();
}
