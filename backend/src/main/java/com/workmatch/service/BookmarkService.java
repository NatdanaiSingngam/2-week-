/*
 * ==== Workmatch · Backend · Service (ตรรกะหลักของระบบ) ====
 * ไฟล์: backend/src/main/java/com/workmatch/service/BookmarkService.java
 * หน้าที่: บันทึก/ยกเลิกบันทึกงาน และรายการงานที่บันทึกไว้
 * เรียกใช้: BookmarkRepository, JobRepository, JobApplicationRepository
 * ถูกเรียกโดย: StudentController
 */
package com.workmatch.service;

import com.workmatch.dto.Views.BookmarkItem;
import com.workmatch.entity.Bookmark;
import com.workmatch.entity.Job;
import com.workmatch.entity.JobStatus;
import com.workmatch.entity.User;
import com.workmatch.exception.JobNotFoundException;
import com.workmatch.repository.BookmarkRepository;
import com.workmatch.repository.JobApplicationRepository;
import com.workmatch.repository.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** งานที่บันทึกไว้ (เอกสาร 1.4.1.2.4, รูปที่ 41) */
@Service
public class BookmarkService {

    private final BookmarkRepository bookmarks;
    private final JobRepository jobs;
    private final JobApplicationRepository applications;

    public BookmarkService(BookmarkRepository bookmarks, JobRepository jobs, JobApplicationRepository applications) {
        this.bookmarks = bookmarks;
        this.jobs = jobs;
        this.applications = applications;
    }

    /** กดบันทึก/ยกเลิก คืน true ถ้าตอนนี้ถูกบันทึกอยู่ */
    @Transactional
    public boolean toggle(User student, Long jobId) {
        return bookmarks.findByStudentIdAndJobId(student.getId(), jobId).map(b -> {
            bookmarks.delete(b);
            return false;
        }).orElseGet(() -> {
            Job job = jobs.findById(jobId).orElseThrow(JobNotFoundException::new);
            if (job.getStatus() == JobStatus.DRAFT) throw new JobNotFoundException();
            Bookmark b = new Bookmark();
            b.setStudent(student);
            b.setJob(job);
            bookmarks.save(b);
            return true;
        });
    }

    @Transactional
    public void remove(User student, Long jobId) {
        bookmarks.findByStudentIdAndJobId(student.getId(), jobId).ifPresent(bookmarks::delete);
    }

    @Transactional(readOnly = true)
    public List<BookmarkItem> list(User student) {
        return bookmarks.findForStudent(student.getId()).stream()
                .filter(b -> b.getJob().getStatus() != JobStatus.DRAFT)
                .map(b -> new BookmarkItem(b.getJob().getId(), b.getJob().getTitle(),
                        b.getJob().getCompany().getDisplayName(), b.getCreatedAt(), b.getJob().isOpen(),
                        applications.existsByStudentIdAndJobId(student.getId(), b.getJob().getId())))
                .toList();
    }
}
