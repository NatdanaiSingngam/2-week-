/*
 * ==== Workmatch · Backend · เข้าถึงฐานข้อมูล (Repository) ====
 * ไฟล์: backend/src/main/java/com/workmatch/repository/BookmarkRepository.java
 * หน้าที่: ค้นหา / บันทึก / ลบข้อมูลในตาราง bookmarks (Spring Data สร้างคำสั่ง SQL จากชื่อเมธอดให้เอง)
 * อ่าน/เขียนตาราง: bookmarks (entity Bookmark) ผ่าน Spring Data JPA
 * ถูกเรียกโดย: BookmarkService, DemoDataLoader, JobPostingService, StudentDashboardController
 */
package com.workmatch.repository;

import com.workmatch.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    Optional<Bookmark> findByStudentIdAndJobId(Long studentId, Long jobId);
    boolean existsByStudentIdAndJobId(Long studentId, Long jobId);

    @Query("select b from Bookmark b join fetch b.job j join fetch j.company where b.student.id = ?1 order by b.createdAt desc")
    List<Bookmark> findForStudent(Long studentId);

    @Query("select b.job.id from Bookmark b where b.student.id = ?1")
    List<Long> findJobIdsForStudent(Long studentId);
}
