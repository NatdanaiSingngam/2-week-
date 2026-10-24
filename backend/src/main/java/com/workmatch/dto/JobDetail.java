package com.workmatch.dto;

import java.util.List;

/**
 * หน้า company/job-detail: {id, title, description, skills}
 * TODO(integration, เอกสาร - ธนวัฒน์ + ชญาณัตถพล): เอกสารสั่งงานเขียนว่า skills เป็น "list ข้อความ"
 *   แต่ job-detail.html ใช้ skill.name จึงส่งเป็น list ของ {name, category} (ใช้ได้แล้ว)
 *   ต้องแก้เอกสารข้อตกลงให้ตรงกับของจริง ไม่ต้องแก้โค้ด
 */
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
