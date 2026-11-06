package com.workmatch.repository;

import com.workmatch.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    Optional<Bookmark> findByStudentIdAndJobId(Long studentId, Long jobId);
    boolean existsByStudentIdAndJobId(Long studentId, Long jobId);

    @Query("select b from Bookmark b join fetch b.job j join fetch j.company where b.student.id = ?1 order by b.createdAt desc")
    List<Bookmark> findForStudent(Long studentId);

    @Query("select b.job.id from Bookmark b where b.student.id = ?1")
    List<Long> findJobIdsForStudent(Long studentId);
}
