/*
 * ==== Workmatch · Backend · ตารางฐานข้อมูล (Entity) ====
 * ไฟล์: backend/src/main/java/com/workmatch/entity/SkillSynonym.java
 * หน้าที่: คำพ้องความหมายของทักษะ (ส่งให้ NLP เป็นคอลัมน์ aliases)
 * ตารางในฐานข้อมูล: skill_synonyms
 * ถูกเรียกโดย: DictionaryService, SkillSynonymRepository
 */
package com.workmatch.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * ตาราง skill_synonyms: คำพ้องความหมาย -> ทักษะมาตรฐาน (เอกสาร 1.4.3.3, รูปที่ 51) เช่น "JS" -> JavaScript
 * ส่งให้ NLP เป็นคอลัมน์ aliases ของ skill_dictionary.csv
 */
@Entity
@Table(name = "skill_synonyms")
public class SkillSynonym {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String alias;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SkillEntry skill;

    public Long getId() { return id; }
    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
    public SkillEntry getSkill() { return skill; }
    public void setSkill(SkillEntry skill) { this.skill = skill; }
}
