package com.workmatch.controller;

import com.workmatch.service.CurrentUserService;
import com.workmatch.service.ResumeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/** หน้า Dashboard นักศึกษา: ตอนนี้บอกได้แค่ว่ามีเรซูเม่หรือยัง งาน A4 จะเติม matches */
@Controller
public class StudentDashboardController {

    private final ResumeService resumeService;
    private final CurrentUserService currentUserService;

    public StudentDashboardController(ResumeService resumeService, CurrentUserService currentUserService) {
        this.resumeService = resumeService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/student/dashboard")
    public String dashboard(Model model) {
        boolean hasResume = resumeService.findResume(currentUserService.getCurrentUser()).isPresent();
        model.addAttribute("hasResume", hasResume);
        model.addAttribute("matches", List.of());
        return "student/dashboard";
    }
}
