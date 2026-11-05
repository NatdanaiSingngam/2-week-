package com.workmatch.repository;

import com.workmatch.entity.ApplicationStatus;
import com.workmatch.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    Optional<JobApplication> findByStudentIdAndJobId(Long studentId, Long jobId);
    boolean existsByStudentIdAndJobId(Long studentId, Long jobId);

    @Query("select a from JobApplication a join fetch a.job j join fetch j.company where a.student.id = ?1 order by a.appliedAt desc")
    List<JobApplication> findForStudent(Long studentId);

    @Query("select a from JobApplication a join fetch a.student where a.job.id = ?1 order by a.appliedAt desc")
    List<JobApplication> findForJob(Long jobId);

    @Query("select a from JobApplication a join fetch a.student join fetch a.job j where j.company.id = ?1 order by a.appliedAt desc")
    List<JobApplication> findForCompany(Long companyId);

    long countByJobId(Long jobId);
    long countByJobCompanyId(Long companyId);
    long countByJobCompanyIdAndStatus(Long companyId, ApplicationStatus status);
    long countByStatus(ApplicationStatus status);

    @Query("select a.appliedAt from JobApplication a where a.appliedAt >= ?1")
    List<LocalDateTime> findAppliedSince(LocalDateTime since);
}
