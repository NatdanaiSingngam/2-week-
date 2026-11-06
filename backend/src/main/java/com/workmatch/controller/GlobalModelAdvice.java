package com.workmatch.controller;

import com.workmatch.entity.Role;
import com.workmatch.repository.CompanyProfileRepository;
import com.workmatch.service.ApplicationService;
import com.workmatch.service.CurrentUserService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * ใส่ข้อมูลผู้ที่ login อยู่ให้ทุกหน้า (ยังไม่ login จะไม่ใส่):
 * displayName, role, userEmail, userInitials (วงกลมมุมขวาบน) และ companyVerified (ป้าย "ยืนยันตัวตนแล้ว" ฝั่งบริษัท)
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private final CurrentUserService currentUserService;
    private final CompanyProfileRepository companyProfiles;

    public GlobalModelAdvice(CurrentUserService currentUserService, CompanyProfileRepository companyProfiles) {
        this.currentUserService = currentUserService;
        this.companyProfiles = companyProfiles;
    }

    @ModelAttribute
    public void addCurrentUser(Model model) {
        currentUserService.findCurrentUser().ifPresent(user -> {
            model.addAttribute("displayName", user.getDisplayName());
            model.addAttribute("role", user.getRole().name());
            model.addAttribute("userEmail", user.getEmail());
            // บริษัทใช้อักษรแรกของชื่อบริษัท (ข้ามคำว่า "บริษัท") นักศึกษาใช้อักษรแรกของชื่อและนามสกุล
            model.addAttribute("userInitials", user.getRole() == Role.COMPANY
                    ? com.workmatch.service.JobPostingService.initial(user.getDisplayName())
                    : ApplicationService.initials(user.getDisplayName()));
            if (user.getRole() == Role.COMPANY) {
                model.addAttribute("companyVerified", companyProfiles.findByUserId(user.getId())
                        .map(p -> p.isVerified()).orElse(false));
            }
        });
    }
}
