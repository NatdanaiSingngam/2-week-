/*
 * ==== Workmatch · Backend · Controller (รับคำขอจากเบราว์เซอร์) ====
 * ไฟล์: backend/src/main/java/com/workmatch/controller/CompanyController.java
 * หน้าที่: หน้าบริษัท: ผู้สมัครทั้งหมด, รายละเอียดผู้สมัคร + เปลี่ยนสถานะ, โปรไฟล์องค์กร + เอกสาร ภ.พ.20
 * URL ที่รับ: GET /company/applications, GET /company/applications/{id},
 *     POST /company/applications/{id}/status, GET /company/applications/{id}/resume, GET /company/profile,
 *     POST /company/profile, GET /company/profile/document
 * แสดงผลที่หน้า: frontend/templates/company/applications.html, frontend/templates/company/candidate.html,
 *     frontend/templates/company/profile.html
 * เรียกใช้: CurrentUserService, ApplicationService, ProfileService, FileStorageService
 */
package com.workmatch.controller;

import com.workmatch.entity.ApplicationStatus;
import com.workmatch.entity.User;
import com.workmatch.service.ApplicationService;
import com.workmatch.service.CurrentUserService;
import com.workmatch.service.FileStorageService;
import com.workmatch.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * ผู้สมัครทั้งหมด (เมนู "ผู้สมัครทั้งหมด"), รายละเอียดผู้สมัคร + อัปเดตสถานะ (เอกสาร รูปที่ 46)
 * และโปรไฟล์องค์กร + ยืนยันตัวตน ภ.พ.20 (รูปที่ 43)
 */
@Controller
public class CompanyController {

    private final CurrentUserService currentUserService;
    private final ApplicationService applicationService;
    private final ProfileService profileService;
    private final FileStorageService fileStorage;

    public CompanyController(CurrentUserService currentUserService, ApplicationService applicationService,
                             ProfileService profileService, FileStorageService fileStorage) {
        this.currentUserService = currentUserService;
        this.applicationService = applicationService;
        this.profileService = profileService;
        this.fileStorage = fileStorage;
    }

    @GetMapping("/company/applications")
    public String applications(@RequestParam(required = false) String status, Model model) {
        User company = currentUserService.getCurrentUser();
        ApplicationStatus filter = null;
        try {
            if (status != null && !status.isBlank()) filter = ApplicationStatus.valueOf(status);
        } catch (IllegalArgumentException ignored) {
            // ค่าไม่ถูกต้อง = แสดงทั้งหมด
        }
        var all = applicationService.forCompany(company, null);
        model.addAttribute("counts", ApplicationService.countByStatus(all));
        model.addAttribute("total", all.size());
        model.addAttribute("applications", filter == null ? all : applicationService.forCompany(company, filter));
        model.addAttribute("statuses", ApplicationStatus.values());
        model.addAttribute("status", filter == null ? "" : filter.name());
        return "company/applications";
    }

    @GetMapping("/company/applications/{id}")
    public String candidate(@PathVariable Long id, Model model) {
        model.addAttribute("c", applicationService.candidate(currentUserService.getCurrentUser(), id));
        return "company/candidate";
    }

    @PostMapping("/company/applications/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam String status, RedirectAttributes redirect) {
        ApplicationStatus s = ApplicationStatus.valueOf(status);
        applicationService.updateStatus(currentUserService.getCurrentUser(), id, s);
        redirect.addFlashAttribute("message", "อัปเดตสถานะเป็น \"" + s.getLabel() + "\" แล้ว");
        return "redirect:/company/applications/" + id;
    }

    @GetMapping("/company/applications/{id}/resume")
    public ResponseEntity<byte[]> resume(@PathVariable Long id) {
        return PdfResponses.inline(applicationService.resumeFileOf(currentUserService.getCurrentUser(), id)
                .flatMap(fileStorage::find));
    }

    @GetMapping("/company/profile")
    public String profile(Model model) {
        User company = currentUserService.getCurrentUser();
        var p = profileService.companyProfile(company);
        model.addAttribute("profile", p);
        model.addAttribute("documentName", fileStorage.fileName(p.getDocumentFileId()).orElse(null));
        return "company/profile";
    }

    @PostMapping("/company/profile")
    public String saveProfile(@RequestParam(required = false) String companyName, @RequestParam(required = false) String taxId,
                              @RequestParam(required = false) String address, @RequestParam(required = false) String businessType,
                              @RequestParam(required = false) String employeeCount,
                              @RequestParam(required = false) String selectionMethod,
                              @RequestParam(name = "document", required = false) MultipartFile document,
                              Model model, RedirectAttributes redirect) {
        User company = currentUserService.getCurrentUser();
        try {
            profileService.updateCompany(company, companyName, taxId, address, businessType, employeeCount,
                    selectionMethod, document);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/company/profile";
        }
        boolean uploaded = document != null && !document.isEmpty();
        redirect.addFlashAttribute("message", uploaded ? "บันทึกข้อมูลและส่งเอกสารแล้ว รอผู้ดูแลระบบอนุมัติ" : "บันทึกข้อมูลแล้ว");
        return "redirect:/company/profile";
    }

    @GetMapping("/company/profile/document")
    public ResponseEntity<byte[]> document() {
        var p = profileService.companyProfile(currentUserService.getCurrentUser());
        return PdfResponses.inline(fileStorage.find(p.getDocumentFileId()));
    }
}
