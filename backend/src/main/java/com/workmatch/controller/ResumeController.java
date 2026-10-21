package com.workmatch.controller;

import com.workmatch.entity.Resume;
import com.workmatch.entity.User;
import com.workmatch.nlp.NlpUnavailableException;
import com.workmatch.service.CurrentUserService;
import com.workmatch.service.ResumeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ResumeController {

    private final ResumeService resumeService;
    private final CurrentUserService currentUserService;

    public ResumeController(ResumeService resumeService, CurrentUserService currentUserService) {
        this.resumeService = resumeService;
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
        try {
            resumeService.saveResume(student, text);
        } catch (NlpUnavailableException | IllegalArgumentException e) {
            // แสดง error บนหน้าเดิม และคงข้อความที่กรอกไว้ ทักษะที่แสดงยังเป็นชุดเดิม
            model.addAttribute("error", e.getMessage());
            model.addAttribute("resumeText", text == null ? "" : text);
            model.addAttribute("skills", resumeService.findSkills(student));
            return "student/resume";
        }
        redirect.addFlashAttribute("message", "บันทึกและวิเคราะห์ทักษะแล้ว");
        return "redirect:/student/resume";
    }
}
