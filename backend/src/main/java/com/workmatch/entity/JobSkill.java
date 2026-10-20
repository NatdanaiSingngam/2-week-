package com.workmatch.entity;

import jakarta.persistence.*;

/** ตาราง job_skills: ทักษะที่ประกาศงานต้องการ (สกัดจาก description) */
@Entity
@Table(name = "job_skills")
public class JobSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "skill_name", nullable = false, length = 100)
    private String skillName;

    @Column(length = 50)
    private String category;

    public JobSkill() {}

    public JobSkill(Job job, String skillName, String category) {
        this.job = job;
        this.skillName = skillName;
        this.category = category;
    }

    public Long getId() { return id; }
    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
