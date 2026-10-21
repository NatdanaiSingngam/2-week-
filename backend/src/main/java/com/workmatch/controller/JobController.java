package com.workmatch.controller;

import com.workmatch.dto.JobForm;
import com.workmatch.entity.User;
import com.workmatch.nlp.NlpUnavailableException;
import com.workmatch.service.CurrentUserService;
import com.workmatch.service.JobService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class JobController {

    private final JobService jobService;
    private final CurrentUserService currentUserService;

    public JobController(JobService jobService, CurrentUserService currentUserService) {
        this.jobService = jobService;
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
        try {
            Long id = jobService.createJob(company, title, description);
            return "redirect:/company/jobs/" + id;
        } catch (NlpUnavailableException | IllegalArgumentException e) {
            model.addAttribute("job", new JobForm(title, description));
            model.addAttribute("error", e.getMessage());
            return "company/job-form";
        }
    }

    @GetMapping("/company/jobs/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("job", jobService.getJobDetail(currentUserService.getCurrentUser(), id));
        model.addAttribute("applicants", List.of()); // งาน A4 จะเติมผู้สมัครเรียงตามคะแนน
        return "company/job-detail";
    }
}
