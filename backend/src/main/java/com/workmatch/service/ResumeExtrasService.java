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

    /** PDF ที่อัปโหลดแล้วแต่ยังไม่บันทึกเป็นเรซูเม่: id ไฟล์ ชื่อไฟล์ และข้อความที่สกัดได้ (ให้ผู้ใช้ตรวจก่อน) */
    public record PendingPdf(Long fileId, String fileName, String text) {
    }

    /**
     * อัปโหลด PDF (เอกสาร UC-06 ข้อ 2-5, รูปที่ 49): อ่านข้อความออกมาให้ผู้ใช้ตรวจ/แก้ก่อน ยังไม่ส่ง NLP และยังไม่บันทึกเรซูเม่
     * เก็บไฟล์ไว้ก่อน (ผูกกับเรซูเม่ตอนกด "บันทึก" ผ่าน saveResume) ไฟล์ที่ค้างจากการอัปโหลดครั้งก่อนจะถูกลบ
     * อ่านข้อความไม่ได้ (เช่น PDF ภาพสแกน) -> แจ้งให้กรอกหรือวาง Raw Text เอง (Exception Flow ของ UC-06)
     */
    @Transactional
    public PendingPdf preparePdf(User student, MultipartFile upload, Long previousPendingId) {
        byte[] data = fileStorage.readPdf(upload);
        String text = fileStorage.extractText(data).trim();
        if (text.isBlank()) {
            throw new IllegalArgumentException("อ่านข้อความจากไฟล์ PDF ไม่ได้ (อาจเป็นภาพสแกน) กรุณากรอกหรือวางข้อความเรซูเม่ (Raw Text) ในช่องด้านล่างแทน");
        }
        if (text.length() > ResumeService.MAX_LENGTH) text = text.substring(0, ResumeService.MAX_LENGTH);
        discardPending(student, previousPendingId);
        StoredFile saved = fileStorage.storePdf(student, upload);
        return new PendingPdf(saved.getId(), saved.getFileName(), text);
    }

    /**
     * ปุ่ม "บันทึก": บันทึกข้อความ (สกัดทักษะด้วย NLP ผ่าน ResumeService) แล้วผูกไฟล์ PDF ที่รออยู่ (ถ้ามี) แทนไฟล์เดิม
     * ไฟล์ที่รออยู่ต้องเป็นของผู้ใช้คนนี้และยังไม่ถูกผูกกับเรซูเม่ใด
     */
    @Transactional
    public Resume saveResume(User student, String text, Long pendingFileId) {
        Resume resume = resumeService.saveResume(student, text);
        if (pendingFileId != null && ownedUnlinked(student, pendingFileId)) {
            Long old = resume.getResumeFileId();
            resume.setResumeFileId(pendingFileId);
            resume = resumes.save(resume);
            if (old != null && !old.equals(pendingFileId)) fileStorage.delete(old);
        }
        return resume;
    }

    /** ลบไฟล์ PDF ที่อัปโหลดค้างไว้แต่ไม่ได้บันทึก (เฉพาะของผู้ใช้คนนี้ที่ยังไม่ผูกกับเรซูเม่) */
    @Transactional
    public void discardPending(User student, Long pendingFileId) {
        if (pendingFileId != null && ownedUnlinked(student, pendingFileId)) fileStorage.delete(pendingFileId);
    }

    @Transactional(readOnly = true)
    public Optional<String> pendingName(User student, Long pendingFileId) {
        if (pendingFileId == null || !ownedUnlinked(student, pendingFileId)) return Optional.empty();
        return fileStorage.fileName(pendingFileId);
    }

    private boolean ownedUnlinked(User student, Long fileId) {
        boolean owned = fileStorage.find(fileId).map(f -> f.getOwner().getId().equals(student.getId())).orElse(false);
        boolean linked = resumes.findByUserId(student.getId()).map(r -> fileId.equals(r.getResumeFileId())).orElse(false);
        return owned && !linked;
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
