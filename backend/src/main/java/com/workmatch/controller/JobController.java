package com.workmatch.controller;

import com.workmatch.dto.JobForm;
import com.workmatch.entity.User;
import com.workmatch.exception.JobNotFoundException;
import com.workmatch.nlp.NlpUnavailableException;
import com.workmatch.service.CurrentUserService;
import com.workmatch.service.JobService;
import com.workmatch.service.MatchService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class JobController {

    private static final Logger log = LoggerFactory.getLogger(JobController.class);

    private final JobService jobService;
    private final MatchService matchService;
    private final CurrentUserService currentUserService;

    public JobController(JobService jobService, MatchService matchService, CurrentUserService currentUserService) {
        this.jobService = jobService;
        this.matchService = matchService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/company/jobs")
    public String list(Model model) {
        model.addAttribute("jobs", jobService.listJobs(currentUserService.getCurrentUser()));
        return "company/jobs";
    }

    @GetMapping("/company/jobs/new")
    public String newForm(Model model) {
        model.addAttribute("job", new JobForm("", ""));
        return "company/job-form";
    }

    @PostMapping("/company/jobs")
    public String create(@RequestParam(required = false) String title,
                         @RequestParam(required = false) String description,
                         Model model) {
        User company = currentUserService.getCurrentUser();
        Long id;
        try {
            id = jobService.createJob(company, title, description);
        } catch (NlpUnavailableException | IllegalArgumentException e) {
            model.addAttribute("job", new JobForm(title, description));
            model.addAttribute("error", e.getMessage());
            return "company/job-form";
        }
        // จับคู่กับ resume ทุกฉบับ ถ้าพังให้เก็บ log เท่านั้น ประกาศงานที่บันทึกแล้วไม่หาย
        try {
            matchService.recalculateForJob(id);
        } catch (RuntimeException e) {
            log.warn("คำนวณคะแนนจับคู่ไม่สำเร็จ (job {})", id, e);
        }
        return "redirect:/company/jobs/" + id;
    }

    @GetMapping("/company/jobs/{id}")
    public String detail(@PathVariable Long id, Model model, HttpServletResponse response) {
        // getJobDetail ตรวจว่าเป็นประกาศของบริษัทนี้ก่อน แล้วค่อยดึงผู้สมัคร
        try {
            model.addAttribute("job", jobService.getJobDetail(currentUserService.getCurrentUser(), id));
        } catch (JobNotFoundException e) {
            // ไม่มีประกาศนี้ หรือเป็นของบริษัทอื่น: ตอบ 404 แต่ใช้หน้า job-detail ส่วน job == null ของชญาณัตถพล
            // ("ไม่พบตำแหน่งงาน" + ปุ่มกลับไปรายการตำแหน่ง) ไม่ส่งข้อมูลงานหรือผู้สมัครใด ๆ ออกไป (TODO ข้อ 3)
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            model.addAttribute("applicants", List.of());
            return "company/job-detail";
        }
        model.addAttribute("applicants", matchService.applicantsFor(id));
        return "company/job-detail";
    }
}
