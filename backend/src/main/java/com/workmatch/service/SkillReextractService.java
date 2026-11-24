/*
 * ==== Workmatch · Backend · Service (ตรรกะหลักของระบบ) ====
 * ไฟล์: backend/src/main/java/com/workmatch/service/SkillReextractService.java
 * หน้าที่: สกัดทักษะใหม่ให้เรซูเม่ทุกฉบับเมื่อแก้พจนานุกรม แล้วคำนวณคะแนนจับคู่ใหม่ (แผน 30 พ.ย., backlog B4)
 *          เดิมทักษะในเรซูเม่เก่าค้างอยู่จนกว่านักศึกษาจะกดบันทึกใหม่ (เช่น เคส Excel ของวิชัย)
 * ถูกเรียกโดย: AdminController (หลังเพิ่ม/แก้/เปิด-ปิดทักษะ หรือเพิ่ม/ลบคำพ้อง และปุ่ม "สกัดทักษะใหม่ทั้งหมด")
 * เรียกใช้: NlpClient (/extract), ResumeRepository, ResumeSkillRepository, MatchService.recalculateAll()
 */
package com.workmatch.service;

import com.workmatch.entity.Resume;
import com.workmatch.entity.ResumeSkill;
import com.workmatch.nlp.ExtractedSkill;
import com.workmatch.nlp.NlpClient;
import com.workmatch.repository.ResumeRepository;
import com.workmatch.repository.ResumeSkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * กติกาการสกัดใหม่ของแต่ละเรซูเม่:
 *   ทักษะใหม่ = ผลของ NLP จากข้อความเดิม − ทักษะที่นักศึกษากดลบเอง (Resume.removedSkills) + ทักษะที่นักศึกษาเพิ่มเอง (is_manual)
 * แทนเฉพาะแถวที่มาจาก NLP ไม่แตะแถวที่เพิ่มเอง ทำทั้งหมดในธุรกรรมเดียว: NLP ล้มกลางทาง = ไม่มีเรซูเม่ใดถูกเปลี่ยน
 */
@Service
public class SkillReextractService {

    /** ผลการสกัดใหม่: เรซูเม่ทั้งหมดที่ตรวจ, เรซูเม่ที่ทักษะเปลี่ยน, คู่คะแนนที่คำนวณใหม่ */
    public record Result(int resumes, int changed, int pairs) { }

    private final ResumeRepository resumes;
    private final ResumeSkillRepository resumeSkills;
    private final NlpClient nlpClient;
    private final MatchService matchService;

    public SkillReextractService(ResumeRepository resumes, ResumeSkillRepository resumeSkills, NlpClient nlpClient,
                                 MatchService matchService) {
        this.resumes = resumes;
        this.resumeSkills = resumeSkills;
        this.nlpClient = nlpClient;
        this.matchService = matchService;
    }

    @Transactional
    public Result reextractAll() {
        int total = 0, changed = 0;
        for (Resume resume : resumes.findAll()) {
            total++;
            if (reextract(resume)) changed++;
        }
        int pairs = changed > 0 ? matchService.recalculateAll() : 0;
        return new Result(total, changed, pairs);
    }

    /** คืน true ถ้าชุดทักษะของเรซูเม่นี้เปลี่ยน */
    private boolean reextract(Resume resume) {
        List<ExtractedSkill> extracted = ResumeService.dedupe(nlpClient.extractSkills(resume.getRawText()));
        Set<String> removed = lower(resume.getRemovedSkillSet());
        List<ResumeSkill> current = resumeSkills.findByResumeId(resume.getId());
        Set<String> manual = lower(current.stream().filter(ResumeSkill::isManual).map(ResumeSkill::getSkillName).toList());

        List<ExtractedSkill> wanted = extracted.stream()
                .filter(s -> !removed.contains(key(s.name())) && !manual.contains(key(s.name())))
                .toList();
        Set<String> before = lower(current.stream().filter(s -> !s.isManual()).map(ResumeSkill::getSkillName).toList());
        Set<String> after = lower(wanted.stream().map(ExtractedSkill::name).toList());
        if (before.equals(after)) return false;

        current.stream().filter(s -> !s.isManual()).forEach(resumeSkills::delete);
        List<ResumeSkill> rows = new ArrayList<>();
        for (ExtractedSkill s : wanted) rows.add(new ResumeSkill(resume, s.name(), s.category()));
        resumeSkills.saveAll(rows);
        return true;
    }

    private static String key(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private static Set<String> lower(java.util.Collection<String> names) {
        return names.stream().map(SkillReextractService::key).collect(Collectors.toCollection(TreeSet::new));
    }
}
