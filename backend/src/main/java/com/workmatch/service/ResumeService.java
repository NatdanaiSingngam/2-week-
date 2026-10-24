package com.workmatch.service;

import com.workmatch.dto.SkillView;
import com.workmatch.entity.Resume;
import com.workmatch.entity.ResumeSkill;
import com.workmatch.entity.User;
import com.workmatch.nlp.ExtractedSkill;
import com.workmatch.nlp.NlpClient;
import com.workmatch.repository.ResumeRepository;
import com.workmatch.repository.ResumeSkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class ResumeService {

    public static final int MAX_LENGTH = 20000;

    private final ResumeRepository resumes;
    private final ResumeSkillRepository resumeSkills;
    private final NlpClient nlpClient;

    public ResumeService(ResumeRepository resumes, ResumeSkillRepository resumeSkills, NlpClient nlpClient) {
        this.resumes = resumes;
        this.resumeSkills = resumeSkills;
        this.nlpClient = nlpClient;
    }

    /**
     * บันทึกเรซูเม่ (1 คนมี 1 ฉบับ) แล้วแทนทักษะชุดเดิมด้วยชุดใหม่จาก NLP
     * เรียก NLP ก่อนแตะฐานข้อมูล ถ้า NLP ล้มเหลวจะไม่มีอะไรถูกแก้ และอยู่ใน transaction เดียวกันทั้งหมด
     */
    @Transactional
    public Resume saveResume(User student, String rawText) {
        String text = rawText == null ? "" : rawText.trim();
        if (text.isEmpty()) throw new IllegalArgumentException("กรุณากรอกข้อความ resume");
        if (text.length() > MAX_LENGTH) throw new IllegalArgumentException("ข้อความ resume ยาวเกิน 20,000 ตัวอักษร");

        List<ExtractedSkill> extracted = nlpClient.extractSkills(text); // ล้มเหลวจะโยน NlpUnavailableException

        Resume resume = resumes.findByUserId(student.getId()).orElseGet(() -> {
            Resume r = new Resume();
            r.setUser(student);
            return r;
        });
        resume.setRawText(text);
        resume.touch();
        resume = resumes.save(resume);

        resumeSkills.deleteByResumeId(resume.getId());
        List<ResumeSkill> rows = new ArrayList<>();
        for (ExtractedSkill s : dedupe(extracted)) {
            rows.add(new ResumeSkill(resume, s.name(), s.category()));
        }
        resumeSkills.saveAll(rows);
        return resume;
    }

    @Transactional(readOnly = true)
    public Optional<Resume> findResume(User student) {
        return resumes.findByUserId(student.getId());
    }

    @Transactional(readOnly = true)
    public List<SkillView> findSkills(User student) {
        return resumes.findByUserId(student.getId())
                .map(r -> resumeSkills.findByResumeId(r.getId()).stream()
                        .map(s -> new SkillView(s.getSkillName(), s.getCategory()))
                        .toList())
                .orElse(List.of());
    }

    /** ตัดชื่อทักษะซ้ำ (ไม่สนตัวพิมพ์เล็กใหญ่) และข้ามชื่อว่าง */
    static List<ExtractedSkill> dedupe(List<ExtractedSkill> skills) {
        Map<String, ExtractedSkill> unique = new LinkedHashMap<>();
        for (ExtractedSkill s : skills) {
            if (s == null || s.name() == null || s.name().isBlank()) continue;
            unique.putIfAbsent(s.name().trim().toLowerCase(Locale.ROOT), new ExtractedSkill(s.name().trim(), s.category()));
        }
        return new ArrayList<>(unique.values());
    }
}
