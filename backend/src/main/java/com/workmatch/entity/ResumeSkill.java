package com.workmatch.entity;

import jakarta.persistence.*;

/** ตาราง resume_skills: ทักษะที่ NLP สกัดได้จากเรซูเม่ */
@Entity
@Table(name = "resume_skills")
public class ResumeSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Column(name = "skill_name", nullable = false, length = 100)
    private String skillName;

    @Column(length = 50)
    private String category;

    public ResumeSkill() {}

    public ResumeSkill(Resume resume, String skillName, String category) {
        this.resume = resume;
        this.skillName = skillName;
        this.category = category;
    }

    public Long getId() { return id; }
    public Resume getResume() { return resume; }
    public void setResume(Resume resume) { this.resume = resume; }
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
