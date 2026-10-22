package com.workmatch.repository;

import com.workmatch.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {
    // นักศึกษา 1 คนมีเรซูเม่ได้ 1 ฉบับ
    Optional<Resume> findByUserId(Long userId);
}
