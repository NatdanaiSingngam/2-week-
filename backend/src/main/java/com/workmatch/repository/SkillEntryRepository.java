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
