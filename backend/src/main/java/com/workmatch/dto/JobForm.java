package com.workmatch.dto;

/** ค่าที่กรอกในฟอร์ม company/job-form: {title, description} */
public class JobForm {
    private final String title;
    private final String description;

    public JobForm(String title, String description) {
        this.title = title == null ? "" : title;
        this.description = description == null ? "" : description;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
}
