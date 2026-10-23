package com.workmatch.dto;

/** ทักษะที่ส่งให้หน้าเว็บ: {name, category} */
public class SkillView {
    private final String name;
    private final String category;

    public SkillView(String name, String category) {
        this.name = name;
        this.category = category;
    }

    public String getName() { return name; }
    public String getCategory() { return category; }
}
