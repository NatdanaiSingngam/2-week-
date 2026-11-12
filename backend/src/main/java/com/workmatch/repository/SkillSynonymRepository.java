/*
 * ==== Workmatch · Backend · เข้าถึงฐานข้อมูล (Repository) ====
 * ไฟล์: backend/src/main/java/com/workmatch/repository/SkillSynonymRepository.java
 * หน้าที่: ค้นหา / บันทึก / ลบข้อมูลในตาราง skill_synonyms (Spring Data สร้างคำสั่ง SQL จากชื่อเมธอดให้เอง)
 * อ่าน/เขียนตาราง: skill_synonyms (entity SkillSynonym) ผ่าน Spring Data JPA
 * ถูกเรียกโดย: DictionaryService
 */
package com.workmatch.repository;

import com.workmatch.entity.SkillSynonym;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SkillSynonymRepository extends JpaRepository<SkillSynonym, Long> {
    Optional<SkillSynonym> findByAliasIgnoreCase(String alias);
    List<SkillSynonym> findAllByOrderByAliasAsc();
    List<SkillSynonym> findBySkillId(Long skillId);
}
