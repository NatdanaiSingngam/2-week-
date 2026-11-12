/*
 * ==== Workmatch · Backend · เข้าถึงฐานข้อมูล (Repository) ====
 * ไฟล์: backend/src/main/java/com/workmatch/repository/StoredFileRepository.java
 * หน้าที่: ค้นหา / บันทึก / ลบข้อมูลในตาราง stored_files (Spring Data สร้างคำสั่ง SQL จากชื่อเมธอดให้เอง)
 * อ่าน/เขียนตาราง: stored_files (entity StoredFile) ผ่าน Spring Data JPA
 * ถูกเรียกโดย: ApplicationService, DemoDataLoader, FileStorageService
 */
package com.workmatch.repository;

import com.workmatch.entity.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
}
