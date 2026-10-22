package com.workmatch.dto;

/** หนึ่งแถวในหน้า company/jobs: {id, title, skillCount, applicantCount} */
public class JobListItem {
    private final Long id;
    private final String title;
    private final long skillCount;
    private final long applicantCount;

    public JobListItem(Long id, String title, long skillCount, long applicantCount) {
        this.id = id;
        this.title = title;
        this.skillCount = skillCount;
        this.applicantCount = applicantCount;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public long getSkillCount() { return skillCount; }
    public long getApplicantCount() { return applicantCount; }
}
