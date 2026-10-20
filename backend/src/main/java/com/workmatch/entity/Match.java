package com.workmatch.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ตาราง matches: ผลจับคู่ระหว่างเรซูเม่กับประกาศงาน
 * คู่ (resume_id, job_id) ห้ามซ้ำ
 * matched_skills / missing_skills เก็บเป็น JSON array ในรูปข้อความ เช่น ["Java","Git"]
 */
@Entity
@Table(name = "matches",
       uniqueConstraints = @UniqueConstraint(name = "uk_matches_resume_job",
                                             columnNames = {"resume_id", "job_id"}))
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal score;

    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal cosine;

    @Column(name = "matched_skills", nullable = false, columnDefinition = "TEXT")
    private String matchedSkills;

    @Column(name = "missing_skills", nullable = false, columnDefinition = "TEXT")
    private String missingSkills;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Resume getResume() { return resume; }
    public void setResume(Resume resume) { this.resume = resume; }
    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public BigDecimal getCosine() { return cosine; }
    public void setCosine(BigDecimal cosine) { this.cosine = cosine; }
    public String getMatchedSkills() { return matchedSkills; }
    public void setMatchedSkills(String matchedSkills) { this.matchedSkills = matchedSkills; }
    public String getMissingSkills() { return missingSkills; }
    public void setMissingSkills(String missingSkills) { this.missingSkills = missingSkills; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
