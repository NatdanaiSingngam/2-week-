package com.workmatch.dto;

import java.util.List;

/** หน้า company/job-detail: {id, title, description, skills} (skills เป็น {name, category} ตามที่หน้าเว็บใช้ skill.name) */
public class JobDetail {
    private final Long id;
    private final String title;
    private final String description;
    private final List<SkillView> skills;

    public JobDetail(Long id, String title, String description, List<SkillView> skills) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.skills = skills;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public List<SkillView> getSkills() { return skills; }
}
