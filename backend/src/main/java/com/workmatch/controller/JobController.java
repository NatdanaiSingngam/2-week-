package com.workmatch.controller;

import com.workmatch.dto.Views.ApplicantItem;
import com.workmatch.dto.Views.JobInput;
import com.workmatch.entity.ApplicationStatus;
import com.workmatch.entity.JobStatus;
import com.workmatch.entity.User;
import com.workmatch.exception.JobNotFoundException;
import com.workmatch.nlp.NlpUnavailableException;
import com.workmatch.service.ApplicationService;
import com.workmatch.service.CurrentUserService;
import com.workmatch.service.DictionaryService;
import com.workmatch.service.JobPostingService;
import com.workmatch.service.JobService;
import com.workmatch.service.MatchService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * หน้าของสถานประกอบการตามเอกสาร: ภาพรวม (รูปที่ 47), จัดการประกาศงาน (รูปที่ 44),
 * สร้าง/แก้ไขประกาศ (รูปที่ 44 ฟอร์ม), ผู้สมัครของประกาศแยกตามสถานะ + ส่งออก CSV (รูปที่ 45)
 */
@Controller
public class JobController {

    private static final Logger log = LoggerFactory.getLogger(JobController.class);

    private final JobService jobService;
    private final MatchService matchService;
    private final CurrentUserService currentUserService;
    private final JobPostingService jobPostingService;
    private final ApplicationService applicationService;
    private final DictionaryService dictionary;

    public JobController(JobService jobService, MatchService matchService, CurrentUserService currentUserService,
                         JobPostingService jobPostingService, ApplicationService applicationService,
                         DictionaryService dictionary) {
        this.jobService = jobService;
        this.matchService = matchService;
        this.currentUserService = currentUserService;
        this.jobPostingService = jobPostingService;
        this.applicationService = applicationService;
        this.dictionary = dictionary;
    }

    /** หน้าหลักบริษัท: jobs (รายการเดิม) + overview (สถิติและประกาศล่าสุด) */
    @GetMapping("/company/jobs")
    public String list(Model model) {
        User company = currentUserService.getCurrentUser();
        model.addAttribute("jobs", jobService.listJobs(company));
        model.addAttribute("overview", jobPostingService.overview(company));
        return "company/jobs";
    }

    @GetMapping("/company/postings")
    public String postings(Model model) {
        model.addAttribute("rows", jobPostingService.companyJobs(currentUserService.getCurrentUser()));
        return "company/postings";
    }

    @GetMapping("/company/jobs/new")
    public String newForm(Model model) {
        return form(model, JobInput.empty(), null);
    }

    @PostMapping("/company/jobs")
    public String create(@RequestParam(required = false) String title,
                         @RequestParam(required = false) String description,
                         @RequestParam(required = false) String compensation,
                         @RequestParam(required = false) String workSchedule,
                         @RequestParam(required = false) String accommodation,
                         @RequestParam(required = false) String transport,
                         @RequestParam(name = "skills", required = false) List<String> skills,
                         @RequestParam(required = false) String action,
                         Model model, RedirectAttributes redirect) {
        User company = currentUserService.getCurrentUser();
        JobInput input = new JobInput(title, description, compensation, workSchedule, accommodation, transport,
                skills == null ? List.of() : skills, "draft".equals(action));
        Long id;
        try {
            id = jobPostingService.create(company, input);
        } catch (NlpUnavailableException | IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return form(model, input, null);
        }
        afterSave(id, input, redirect, input.draft() ? "บันทึกแบบร่างแล้ว" : "เผยแพร่ประกาศงานแล้ว");
        return "redirect:/company/jobs/" + id;
    }

    @GetMapping("/company/jobs/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        return form(model, jobPostingService.inputFor(currentUserService.getCurrentUser(), id), id);
    }

    @PostMapping("/company/jobs/{id}")
    public String update(@PathVariable Long id,
                         @RequestParam(required = false) String title,
                         @RequestParam(required = false) String description,
                         @RequestParam(required = false) String compensation,
                         @RequestParam(required = false) String workSchedule,
                         @RequestParam(required = false) String accommodation,
                         @RequestParam(required = false) String transport,
                         @RequestParam(name = "skills", required = false) List<String> skills,
                         @RequestParam(required = false) String action,
                         Model model, RedirectAttributes redirect) {
        JobInput input = new JobInput(title, description, compensation, workSchedule, accommodation, transport,
                skills == null ? List.of() : skills, "draft".equals(action));
        try {
            jobPostingService.update(currentUserService.getCurrentUser(), id, input);
        } catch (NlpUnavailableException | IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return form(model, input, id);
        }
        afterSave(id, input, redirect, input.draft() ? "บันทึกแบบร่างแล้ว" : "บันทึกการแก้ไขแล้ว");
        return "redirect:/company/jobs/" + id;
    }

    @PostMapping("/company/jobs/{id}/status")
    public String status(@PathVariable Long id, @RequestParam String status, RedirectAttributes redirect) {
        JobStatus s = JobStatus.valueOf(status);
        jobPostingService.setStatus(currentUserService.getCurrentUser(), id, s);
        if (s == JobStatus.OPEN) recalculate(id, redirect);
        redirect.addFlashAttribute("message", "เปลี่ยนสถานะเป็น \"" + s.getLabel() + "\" แล้ว");
        return "redirect:/company/postings";
    }

    @PostMapping("/company/jobs/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        jobPostingService.delete(currentUserService.getCurrentUser(), id);
        redirect.addFlashAttribute("message", "ลบประกาศงานแล้ว");
        return "redirect:/company/postings";
    }

    @GetMapping("/company/jobs/{id}")
    public String detail(@PathVariable Long id, @RequestParam(required = false) String tab, Model model,
                         HttpServletResponse response) {
        User company = currentUserService.getCurrentUser();
        // getJobDetail ตรวจว่าเป็นประกาศของบริษัทนี้ก่อน แล้วค่อยดึงผู้สมัคร
        try {
            model.addAttribute("job", jobService.getJobDetail(company, id));
        } catch (JobNotFoundException e) {
            // ไม่มีประกาศนี้ หรือเป็นของบริษัทอื่น: ตอบ 404 แต่ใช้หน้า job-detail ส่วน job == null
            // ("ไม่พบตำแหน่งงาน" + ปุ่มกลับไปรายการตำแหน่ง) ไม่ส่งข้อมูลงานหรือผู้สมัครใด ๆ ออกไป (TODO ข้อ 3)
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            model.addAttribute("applicants", List.of());
            return "company/job-detail";
        }
        // applicants = นักศึกษาที่ระบบจับคู่ได้ (แท็บ "ระบบแนะนำ"), applications = ผู้ที่กดสมัครจริง แยกตามสถานะ
        List<ApplicantItem> applications = applicationService.forJob(company, id);
        String current = tab;
        if (current == null || current.isBlank()) current = applications.isEmpty() ? "RECOMMENDED" : "PENDING";
        final String selected = current;
        model.addAttribute("applicants", matchService.applicantsFor(id));
        model.addAttribute("applications", applications.stream().filter(a -> a.status().name().equals(selected)).toList());
        model.addAttribute("counts", ApplicationService.countByStatus(applications));
        model.addAttribute("statuses", ApplicationStatus.values());
        model.addAttribute("tab", selected);
        model.addAttribute("jobStatus", jobPostingService.companyJobs(company).stream()
                .filter(r -> r.id().equals(id)).map(r -> r.status()).findFirst().orElse(JobStatus.OPEN));
        return "company/job-detail";
    }

    /** ส่งออกรายชื่อผู้สมัครเป็น CSV (รูปที่ 45 ปุ่ม "ส่งออกข้อมูล (CSV)") */
    @GetMapping("/company/jobs/{id}/applicants.csv")
    public ResponseEntity<byte[]> csv(@PathVariable Long id) {
        String csv = applicationService.csv(currentUserService.getCurrentUser(), id);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("applicants-job-" + id + ".csv").build().toString())
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    private String form(Model model, JobInput input, Long editingId) {
        model.addAttribute("form", input);
        model.addAttribute("editingId", editingId);
        model.addAttribute("dictionarySkills", dictionary.activeSkills());
        return "company/job-form";
    }

    private void afterSave(Long id, JobInput input, RedirectAttributes redirect, String message) {
        redirect.addFlashAttribute("message", message);
        if (!input.draft()) recalculate(id, redirect);
    }

    // จับคู่กับ resume ทุกฉบับ ถ้าพังให้เก็บ log เท่านั้น ประกาศงานที่บันทึกแล้วไม่หาย
    private void recalculate(Long id, RedirectAttributes redirect) {
        try {
            jobPostingService.recalculate(id);
        } catch (RuntimeException e) {
            log.warn("คำนวณคะแนนจับคู่ไม่สำเร็จ (job {})", id, e);
            redirect.addFlashAttribute("error", "บันทึกประกาศแล้ว แต่ยังคำนวณคะแนนจับคู่ไม่ได้ ลองบันทึกอีกครั้งภายหลัง");
        }
    }
}
