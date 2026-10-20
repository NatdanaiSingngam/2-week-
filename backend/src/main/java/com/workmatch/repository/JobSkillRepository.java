package com.workmatch.repository;

import com.workmatch.entity.JobSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface JobSkillRepository extends JpaRepository<JobSkill, Long> {
    List<JobSkill> findByJobId(Long jobId);

    @Transactional
    void deleteByJobId(Long jobId);
}
