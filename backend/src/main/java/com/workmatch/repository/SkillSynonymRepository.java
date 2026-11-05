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
