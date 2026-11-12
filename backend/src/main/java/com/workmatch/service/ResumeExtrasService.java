/*
 * ==== Workmatch · Backend · Service (ตรรกะหลักของระบบ) ====
 * ไฟล์: backend/src/main/java/com/workmatch/service/ResumeExtrasService.java
 * หน้าที่: ส่วนเสริมเรซูเม่: อัปโหลด PDF -> ดึงข้อความ -> สกัดทักษะ, เพิ่ม/ลบทักษะเอง
 * เรียกใช้: ResumeService, ResumeRepository, ResumeSkillRepository, FileStorageService, DictionaryService
 * ถูกเรียกโดย: ResumeController
 */
package com.workmatch.service;

import com.workmatch.entity.Resume;
import com.workmatch.entity.ResumeSkill;
import com.workmatch.entity.SkillEntry;
import com.workmatch.entity.StoredFile;
import com.workmatch.entity.User;
import com.workmatch.repository.ResumeRepository;
import com.workmatch.repository.ResumeSkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

/**
 * ส่วนเสริมของเรซูเม่ตามเอกสาร รูปที่ 37: อัปโหลด PDF แล้วสกัดทักษะ, ลบ/เพิ่มทักษะเองก่อนบันทึก
 * (แยกจาก ResumeService เดิมเพื่อไม่กระทบชุดทดสอบเดิม)
 */
@Service
public class ResumeExtrasService {

    private final ResumeService resumeService;
    private final ResumeRepository resumes;
    private final ResumeSkillRepository resumeSkills;
    private final FileStorageService fileStorage;
    private final DictionaryService dictionary;

    public ResumeExtrasService(ResumeService resumeService, ResumeRepository resumes, ResumeSkillRepository resumeSkills,
                               FileStorageService fileStorage, DictionaryService dictionary) {
        this.resumeService = resumeService;
        this.resumes = resumes;
        this.resumeSkills = resumeSkills;
        this.fileStorage = fileStorage;
        this.dictionary = dictionary;
    }

    /** อัปโหลด PDF: อ่านข้อความ -> บันทึกเรซูเม่ผ่าน ResumeService (สกัดทักษะด้วย NLP) -> เก็บไฟล์ไว้ให้บริษัทดาวน์โหลด */
    @Transactional
    public Resume uploadPdf(User student, MultipartFile upload) {
        byte[] data = fileStorage.readPdf(upload);
        String text = fileStorage.extractText(data);
        if (text.isBlank()) {
            throw new IllegalArgumentException("ไม่พบข้อความในไฟล์ PDF (อาจเป็นภาพสแกน) แนะนำให้ใช้ไฟล์ PDF แบบ Text-based หรือกรอกข้อความเอง");
        }
        if (text.length() > ResumeService.MAX_LENGTH) text = text.substring(0, ResumeService.MAX_LENGTH);
        Resume resume = resumeService.saveResume(student, text);
        StoredFile saved = fileStorage.storePdf(student, upload);
        Long old = resume.getResumeFileId();
        resume.setResumeFileId(saved.getId());
        resume = resumes.save(resume);
        fileStorage.delete(old);
        return resume;
    }

    /** ลบไฟล์ PDF ที่แนบ (ข้อความและทักษะยังอยู่) */
    @Transactional
    public void removePdf(User student) {
        resumes.findByUserId(student.getId()).ifPresent(r -> {
            Long old = r.getResumeFileId();
            r.setResumeFileId(null);
            resumes.save(r);
            fileStorage.delete(old);
        });
    }

    @Transactional(readOnly = true)
    public Optional<String> pdfName(User student) {
        return resumes.findByUserId(student.getId()).flatMap(r -> fileStorage.fileName(r.getResumeFileId()));
    }

    @Transactional(readOnly = true)
    public Optional<Long> pdfId(User student) {
        return resumes.findByUserId(student.getId()).map(Resume::getResumeFileId);
    }

    /** เพิ่มทักษะเอง: ต้องเป็นคำในพจนานุกรม (หรือคำพ้อง) เพื่อให้จับคู่กับประกาศงานได้ คืนรหัสเรซูเม่ */
    @Transactional
    public Long addSkill(User student, String name) {
        Resume resume = requireResume(student);
        SkillEntry skill = dictionary.resolve(name).orElseThrow(() ->
                new IllegalArgumentException("ไม่พบ \"" + (name == null ? "" : name.trim()) + "\" ในพจนานุกรมทักษะ ลองใช้ชื่อมาตรฐาน เช่น SQL, Figma"));
        boolean exists = resumeSkills.findByResumeId(resume.getId()).stream()
                .anyMatch(s -> s.getSkillName().equalsIgnoreCase(skill.getName()));
        if (exists) throw new IllegalArgumentException("มีทักษะ " + skill.getName() + " อยู่แล้ว");
        resumeSkills.save(new ResumeSkill(resume, skill.getName(), skill.getCategory()));
        return resume.getId();
    }

    /** ลบทักษะที่ระบบสกัดผิด คืนรหัสเรซูเม่ */
    @Transactional
    public Long removeSkill(User student, String name) {
        Resume resume = requireResume(student);
        resumeSkills.findByResumeId(resume.getId()).stream()
                .filter(s -> s.getSkillName().equalsIgnoreCase(name == null ? "" : name.trim()))
                .forEach(resumeSkills::delete);
        return resume.getId();
    }

    private Resume requireResume(User student) {
        return resumes.findByUserId(student.getId())
                .orElseThrow(() -> new IllegalArgumentException("กรุณาบันทึกเรซูเม่ก่อนแก้ไขทักษะ"));
    }
}
