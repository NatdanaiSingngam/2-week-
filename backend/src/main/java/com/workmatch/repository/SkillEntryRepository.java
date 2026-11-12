/*
 * ==== Workmatch · Backend · เข้าถึงฐานข้อมูล (Repository) ====
 * ไฟล์: backend/src/main/java/com/workmatch/repository/SkillEntryRepository.java
 * หน้าที่: ค้นหา / บันทึก / ลบข้อมูลในตาราง skill_dictionary (Spring Data สร้างคำสั่ง SQL จากชื่อเมธอดให้เอง)
 * อ่าน/เขียนตาราง: skill_dictionary (entity SkillEntry) ผ่าน Spring Data JPA
 * ถูกเรียกโดย: DictionaryService
 */
package com.workmatch.repository;

import com.workmatch.entity.SkillEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SkillEntryRepository extends JpaRepository<SkillEntry, Long> {
    Optional<SkillEntry> findByNameIgnoreCase(String name);
    List<SkillEntry> findAllByOrderByIdAsc();
    List<SkillEntry> findByActiveTrueOrderByNameAsc();
    List<SkillEntry> findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCaseOrderByIdAsc(String name, String category);
}
