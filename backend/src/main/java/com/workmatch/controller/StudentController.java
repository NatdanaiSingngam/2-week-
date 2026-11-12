/*
 * ==== Workmatch · Backend · Controller (รับคำขอจากเบราว์เซอร์) ====
 * ไฟล์: backend/src/main/java/com/workmatch/controller/StudentController.java
 * หน้าที่: หน้านักศึกษา: ค้นหางาน, รายละเอียดงาน, บันทึกงาน, สมัครงาน, ประวัติการสมัคร, ข้อมูลส่วนตัว
 * URL ที่รับ: GET /student/jobs, GET /student/jobs/{id}, POST /student/jobs/{id}/bookmark,
 *     POST /student/jobs/{id}/apply, GET /student/bookmarks, POST /student/bookmarks/{jobId}/remove,
 *     GET /student/applications, GET /student/profile, POST /student/profile
 * แสดงผลที่หน้า: frontend/templates/student/applications.html, frontend/templates/student/bookmarks.html,
 *     frontend/templates/student/job.html, frontend/templates/student/jobs.html,
 *     frontend/templates/student/profile.html
 * เรียกใช้: CurrentUserService, JobPostingService, BookmarkService, ApplicationService, ProfileService
 */
package com.workmatch.controller;

import com.workmatch.entity.User;
import com.workmatch.service.ApplicationService;
import com.workmatch.service.BookmarkService;
import com.workmatch.service.CurrentUserService;
import com.workmatch.service.JobPostingService;
import com.workmatch.service.ProfileService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * หน้าของนักศึกษาตามเอกสาร: ค้นหางาน (รูปที่ 38), รายละเอียดงาน + บันทึก/สมัคร (รูปที่ 39),
 * งานที่บันทึกไว้ (รูปที่ 41), ประวัติการสมัครงาน (รูปที่ 42), ข้อมูลส่วนตัว (รูปที่ 36)
 */
@Controller
public class StudentController {

    private final CurrentUserService currentUserService;
    private final JobPostingService jobPostingService;
    private final BookmarkService bookmarkService;
    private final ApplicationService applicationService;
    private final ProfileService profileService;

    public StudentController(CurrentUserService currentUserService, JobPostingService jobPostingService,
                             BookmarkService bookmarkService, ApplicationService applicationService,
                             ProfileService profileService) {
        this.currentUserService = currentUserService;
        this.jobPostingService = jobPostingService;
        this.bookmarkService = bookmarkService;
        this.applicationService = applicationService;
        this.profileService = profileService;
    }

    @GetMapping("/student/jobs")
    public String explore(@RequestParam(required = false) String q, @RequestParam(required = false) String type, Model model) {
        User student = currentUserService.getCurrentUser();
        model.addAttribute("jobs", jobPostingService.explore(student, q, type));
        model.addAttribute("businessTypes", jobPostingService.businessTypes());
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("type", type == null ? "" : type);
        return "student/jobs";
    }

    @GetMapping("/student/jobs/{id}")
    public String job(@PathVariable Long id, Model model) {
        model.addAttribute("job", jobPostingService.studentView(currentUserService.getCurrentUser(), id));
        return "student/job";
    }

    /** บันทึก/ยกเลิกการบันทึก แล้วกลับไปหน้าที่กดมา (รับเฉพาะ path ของนักศึกษา กัน open redirect) */
    @PostMapping("/student/jobs/{id}/bookmark")
    public String toggleBookmark(@PathVariable Long id, @RequestParam(required = false) String back,
                                 RedirectAttributes redirect) {
        boolean saved = bookmarkService.toggle(currentUserService.getCurrentUser(), id);
        redirect.addFlashAttribute("message", saved ? "บันทึกงานนี้แล้ว" : "นำออกจากรายการที่บันทึกแล้ว");
        return "redirect:" + safeBack(back, "/student/jobs/" + id);
    }

    @PostMapping("/student/jobs/{id}/apply")
    public String apply(@PathVariable Long id, @RequestParam(required = false) String coverLetter,
                        RedirectAttributes redirect) {
        try {
            applicationService.apply(currentUserService.getCurrentUser(), id, coverLetter);
            redirect.addFlashAttribute("message", "ส่งใบสมัครแล้ว ติดตามสถานะได้ที่ \"ประวัติการสมัครงาน\"");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            redirect.addFlashAttribute("coverLetter", coverLetter);
        }
        return "redirect:/student/jobs/" + id;
    }

    @GetMapping("/student/bookmarks")
    public String bookmarks(Model model) {
        model.addAttribute("bookmarks", bookmarkService.list(currentUserService.getCurrentUser()));
        model.addAttribute("section", "bookmarks");
        return "student/bookmarks";
    }

    @PostMapping("/student/bookmarks/{jobId}/remove")
    public String removeBookmark(@PathVariable Long jobId, RedirectAttributes redirect) {
        bookmarkService.remove(currentUserService.getCurrentUser(), jobId);
        redirect.addFlashAttribute("message", "นำออกจากรายการที่บันทึกแล้ว");
        return "redirect:/student/bookmarks";
    }

    @GetMapping("/student/applications")
    public String applications(Model model) {
        model.addAttribute("applications", applicationService.forStudent(currentUserService.getCurrentUser()));
        model.addAttribute("section", "applications");
        return "student/applications";
    }

    @GetMapping("/student/profile")
    public String profile(Model model) {
        User student = currentUserService.getCurrentUser();
        model.addAttribute("profile", profileService.studentProfile(student));
        model.addAttribute("email", student.getEmail());
        model.addAttribute("section", "profile");
        return "student/profile";
    }

    @PostMapping("/student/profile")
    public String saveProfile(@RequestParam(required = false) String fullName, @RequestParam(required = false) String phone,
                              @RequestParam(required = false) String studentId, @RequestParam(required = false) String faculty,
                              @RequestParam(required = false) String major, Model model, RedirectAttributes redirect) {
        User student = currentUserService.getCurrentUser();
        try {
            profileService.updateStudent(student, fullName, phone, studentId, faculty, major);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("profile", profileService.studentProfile(student));
            model.addAttribute("email", student.getEmail());
            model.addAttribute("section", "profile");
            return "student/profile";
        }
        redirect.addFlashAttribute("message", "บันทึกข้อมูลแล้ว");
        return "redirect:/student/profile";
    }

    static String safeBack(String back, String fallback) {
        if (back != null && back.matches("^/student/[A-Za-z0-9/_\\-]*(\\?[A-Za-z0-9=&%_.\\-]*)?$")) return back;
        return fallback;
    }
}
