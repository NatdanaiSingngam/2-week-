/*
 * ==== Workmatch · Backend · เข้าถึงฐานข้อมูล (Repository) ====
 * ไฟล์: backend/src/main/java/com/workmatch/repository/ErrorLogRepository.java
 * หน้าที่: ค้นหา / บันทึก / ลบข้อมูลในตาราง error_logs (Spring Data สร้างคำสั่ง SQL จากชื่อเมธอดให้เอง)
 * อ่าน/เขียนตาราง: error_logs (entity ErrorLog) ผ่าน Spring Data JPA
 * ถูกเรียกโดย: ErrorLogService
 */
package com.workmatch.repository;

import com.workmatch.entity.ErrorLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ErrorLogRepository extends JpaRepository<ErrorLog, Long> {
    List<ErrorLog> findTop200ByOrderByOccurredAtDesc();
}
