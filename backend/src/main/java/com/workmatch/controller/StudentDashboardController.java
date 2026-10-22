package com.workmatch.controller;

import com.workmatch.entity.User;
import com.workmatch.service.CurrentUserService;
import com.workmatch.service.MatchService;
import com.workmatch.service.ResumeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** หน้า Dashboard นักศึกษา: hasResume และ matches เรียงคะแนนมากไปน้อย */
@Controller
public class StudentDashboardController {

    private final ResumeService resumeService;
    private final MatchService matchService;
    private final CurrentUserService currentUserService;

    public StudentDashboardController(ResumeService resumeService, MatchService matchService,
                                      CurrentUserService currentUserService) {
        this.resumeService = resumeService;
        this.matchService = matchService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/student/dashboard")
    public String dashboard(Model model) {
        User student = currentUserService.getCurrentUser();
        model.addAttribute("hasResume", resumeService.findResume(student).isPresent());
        model.addAttribute("matches", matchService.dashboardFor(student));
        return "student/dashboard";
    }
}
