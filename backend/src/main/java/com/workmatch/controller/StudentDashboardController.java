package com.workmatch.controller;

import com.workmatch.entity.User;
import com.workmatch.repository.BookmarkRepository;
import com.workmatch.service.CurrentUserService;
import com.workmatch.service.MatchService;
import com.workmatch.service.ProfileService;
import com.workmatch.service.ResumeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashSet;

/**
 * หน้า Dashboard นักศึกษา (เอกสาร รูปที่ 40): hasResume, profile (รหัสนิสิต คณะ สาขา), skills (การ์ดทักษะด้านซ้าย),
 * matches เรียงคะแนนมากไปน้อย และ bookmarkedIds (ปุ่ม "บันทึก" บนการ์ด)
 */
@Controller
public class StudentDashboardController {

    private final ResumeService resumeService;
    private final MatchService matchService;
    private final CurrentUserService currentUserService;
    private final ProfileService profileService;
    private final BookmarkRepository bookmarks;

    public StudentDashboardController(ResumeService resumeService, MatchService matchService,
                                      CurrentUserService currentUserService, ProfileService profileService,
                                      BookmarkRepository bookmarks) {
        this.resumeService = resumeService;
        this.matchService = matchService;
        this.currentUserService = currentUserService;
        this.profileService = profileService;
        this.bookmarks = bookmarks;
    }

    @GetMapping("/student/dashboard")
    public String dashboard(Model model) {
        User student = currentUserService.getCurrentUser();
        model.addAttribute("hasResume", resumeService.findResume(student).isPresent());
        model.addAttribute("skills", resumeService.findSkills(student));
        model.addAttribute("matches", matchService.dashboardFor(student));
        model.addAttribute("profile", profileService.studentProfile(student));
        model.addAttribute("bookmarkedIds", new HashSet<>(bookmarks.findJobIdsForStudent(student.getId())));
        return "student/dashboard";
    }
}
