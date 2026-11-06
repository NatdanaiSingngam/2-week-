package com.workmatch.controller;

import com.workmatch.entity.Resume;
import com.workmatch.entity.User;
import com.workmatch.nlp.NlpUnavailableException;
import com.workmatch.service.CurrentUserService;
import com.workmatch.service.DictionaryService;
import com.workmatch.service.FileStorageService;
import com.workmatch.service.MatchService;
import com.workmatch.service.ResumeExtrasService;
import com.workmatch.service.ResumeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** หน้าจัดการเรซูเม่ (เอกสาร รูปที่ 37): กรอกข้อความ / อัปโหลด PDF / ลบ-เพิ่มทักษะ แล้วคำนวณคะแนนจับคู่ใหม่ */
@Controller
public class ResumeController {

    private static final Logger log = LoggerFactory.getLogger(ResumeController.class);

    private final ResumeService resumeService;
    private final MatchService matchService;
    private final CurrentUserService currentUserService;
    private final ResumeExtrasService resumeExtras;
    private final FileStorageService fileStorage;
    private final DictionaryService dictionary;

    public ResumeController(ResumeService resumeService, MatchService matchService, CurrentUserService currentUserService,
                            ResumeExtrasService resumeExtras, FileStorageService fileStorage, DictionaryService dictionary) {
        this.resumeService = resumeService;
        this.matchService = matchService;
        this.currentUserService = currentUserService;
        this.resumeExtras = resumeExtras;
        this.fileStorage = fileStorage;
        this.dictionary = dictionary;
    }

    @GetMapping("/student/resume")
    public String show(Model model) {
        User student = currentUserService.getCurrentUser();
        model.addAttribute("resumeText", resumeService.findResume(student).map(Resume::getRawText).orElse(""));
        model.addAttribute("skills", resumeService.findSkills(student));
        addExtras(student, model);
        // message มาจาก flash หลังบันทึกสำเร็จ (ถ้าไม่มี หน้าเว็บจะไม่แสดง)
        return "student/resume";
    }

    @PostMapping("/student/resume")
    public String save(@RequestParam(name = "text", required = false) String text,
                       Model model, RedirectAttributes redirect) {
        User student = currentUserService.getCurrentUser();
        Resume saved;
        try {
            saved = resumeService.saveResume(student, text);
        } catch (NlpUnavailableException | IllegalArgumentException e) {
            // แสดง error บนหน้าเดิม และคงข้อความที่กรอกไว้ ทักษะที่แสดงยังเป็นชุดเดิม
            model.addAttribute("error", e.getMessage());
            model.addAttribute("resumeText", text == null ? "" : text);
            model.addAttribute("skills", resumeService.findSkills(student));
            addExtras(student, model);
            return "student/resume";
        }
        redirect.addFlashAttribute("message", "บันทึกและวิเคราะห์ทักษะแล้ว");
        recalculate(saved.getId(), redirect);
        return "redirect:/student/resume";
    }

    /** อัปโหลดไฟล์ PDF: อ่านข้อความแล้วสกัดทักษะด้วย NLP (แทนข้อความเดิม) */
    @PostMapping("/student/resume/upload")
    public String upload(@RequestParam(name = "file", required = false) MultipartFile file, RedirectAttributes redirect) {
        User student = currentUserService.getCurrentUser();
        try {
            Resume saved = resumeExtras.uploadPdf(student, file);
            redirect.addFlashAttribute("message", "อัปโหลดและสกัดทักษะจากไฟล์ PDF แล้ว");
            recalculate(saved.getId(), redirect);
        } catch (NlpUnavailableException | IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/resume";
    }

    @PostMapping("/student/resume/file/delete")
    public String deleteFile(RedirectAttributes redirect) {
        resumeExtras.removePdf(currentUserService.getCurrentUser());
        redirect.addFlashAttribute("message", "ลบไฟล์ PDF แล้ว (ข้อความและทักษะยังอยู่)");
        return "redirect:/student/resume";
    }

    /** เปิดไฟล์ PDF ของตัวเอง */
    @GetMapping("/student/resume/file")
    public ResponseEntity<byte[]> file() {
        User student = currentUserService.getCurrentUser();
        return PdfResponses.inline(resumeExtras.pdfId(student).flatMap(fileStorage::find));
    }

    @PostMapping("/student/resume/skills/add")
    public String addSkill(@RequestParam(required = false) String name, RedirectAttributes redirect) {
        try {
            Long resumeId = resumeExtras.addSkill(currentUserService.getCurrentUser(), name);
            redirect.addFlashAttribute("message", "เพิ่มทักษะแล้ว");
            recalculate(resumeId, redirect);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/resume";
    }

    @PostMapping("/student/resume/skills/remove")
    public String removeSkill(@RequestParam(required = false) String name, RedirectAttributes redirect) {
        try {
            Long resumeId = resumeExtras.removeSkill(currentUserService.getCurrentUser(), name);
            redirect.addFlashAttribute("message", "ลบทักษะ " + (name == null ? "" : name) + " แล้ว");
            recalculate(resumeId, redirect);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/resume";
    }

    private void addExtras(User student, Model model) {
        model.addAttribute("pdfName", resumeExtras.pdfName(student).orElse(null));
        model.addAttribute("dictionarySkills", dictionary.activeSkills());
    }

    /** คำนวณคะแนนจับคู่ใหม่ ถ้าพังให้เก็บ log และเตือน แต่สิ่งที่บันทึกแล้วไม่หาย */
    private void recalculate(Long resumeId, RedirectAttributes redirect) {
        try {
            matchService.recalculateForResume(resumeId);
        } catch (RuntimeException e) {
            log.warn("คำนวณคะแนนจับคู่ไม่สำเร็จ (resume {})", resumeId, e);
            redirect.addFlashAttribute("error", "บันทึก resume แล้ว แต่ยังคำนวณคะแนนจับคู่ไม่ได้ ลองบันทึกอีกครั้งภายหลัง");
        }
    }
}
