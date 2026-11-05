package com.workmatch.repository;

import com.workmatch.entity.ErrorLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ErrorLogRepository extends JpaRepository<ErrorLog, Long> {
    List<ErrorLog> findTop200ByOrderByOccurredAtDesc();
}
