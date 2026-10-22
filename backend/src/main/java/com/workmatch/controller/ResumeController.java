package com.workmatch.controller;

import com.workmatch.entity.Resume;
import com.workmatch.entity.User;
import com.workmatch.nlp.NlpUnavailableException;
import com.workmatch.service.CurrentUserService;
import com.workmatch.service.MatchService;
import com.workmatch.service.ResumeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ResumeController {

    private static final Logger log = LoggerFactory.getLogger(ResumeController.class);

    private final ResumeService resumeService;
    private final MatchService matchService;
    private final CurrentUserService currentUserService;

    public ResumeController(ResumeService resumeService, MatchService matchService, CurrentUserService currentUserService) {
        this.resumeService = resumeService;
        this.matchService = matchService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/student/resume")
    public String show(Model model) {
        User student = currentUserService.getCurrentUser();
        model.addAttribute("resumeText", resumeService.findResume(student).map(Resume::getRawText).orElse(""));
        model.addAttribute("skills", resumeService.findSkills(student));
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
            return "student/resume";
        }
        redirect.addFlashAttribute("message", "บันทึกและวิเคราะห์ทักษะแล้ว");
        // คำนวณคะแนนจับคู่ใหม่ ถ้าพังให้เก็บ log และเตือน แต่ resume ที่บันทึกแล้วไม่หาย
        try {
            matchService.recalculateForResume(saved.getId());
        } catch (RuntimeException e) {
            log.warn("คำนวณคะแนนจับคู่ไม่สำเร็จ (resume {})", saved.getId(), e);
            redirect.addFlashAttribute("error", "บันทึก resume แล้ว แต่ยังคำนวณคะแนนจับคู่ไม่ได้ ลองบันทึกอีกครั้งภายหลัง");
        }
        return "redirect:/student/resume";
    }
}
