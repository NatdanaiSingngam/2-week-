package com.workmatch.repository;

import com.workmatch.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByResumeId(Long resumeId);
    List<Match> findByJobId(Long jobId);
    Optional<Match> findByResumeIdAndJobId(Long resumeId, Long jobId);
}
