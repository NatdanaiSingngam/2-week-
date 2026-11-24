/*
 * ==== Workmatch · Backend · เข้าถึงฐานข้อมูล (Repository) ====
 * ไฟล์: backend/src/main/java/com/workmatch/repository/StoredFileRepository.java
 * หน้าที่: ค้นหา / บันทึก / ลบข้อมูลในตาราง stored_files (Spring Data สร้างคำสั่ง SQL จากชื่อเมธอดให้เอง)
 * อ่าน/เขียนตาราง: stored_files (entity StoredFile) ผ่าน Spring Data JPA
 * ถูกเรียกโดย: ApplicationService, DemoDataLoader, FileStorageService, StoredFileCleanupService
 */
package com.workmatch.repository;

import com.workmatch.entity.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {

    /**
     * ไฟล์ที่ค้าง (แผน 1 ธ.ค., backlog B4): อัปโหลดก่อนเวลา cutoff และไม่มีเรซูเม่หรือโปรไฟล์บริษัทใดอ้างถึง
     * เลือกเฉพาะ id ไม่ดึงเนื้อไฟล์ (LONGBLOB) ขึ้นมา
     */
    @Query("select f.id from StoredFile f where f.createdAt < ?1"
            + " and f.id not in (select r.resumeFileId from Resume r where r.resumeFileId is not null)"
            + " and f.id not in (select c.documentFileId from CompanyProfile c where c.documentFileId is not null)")
    List<Long> findOrphanIds(LocalDateTime cutoff);
}
