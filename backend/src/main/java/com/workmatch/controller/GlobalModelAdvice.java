package com.workmatch.controller;

import com.workmatch.service.CurrentUserService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** ใส่ displayName และ role ของผู้ที่ login อยู่ให้ทุกหน้า (ยังไม่ login จะไม่ใส่) */
@ControllerAdvice
public class GlobalModelAdvice {

    private final CurrentUserService currentUserService;

    public GlobalModelAdvice(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @ModelAttribute
    public void addCurrentUser(Model model) {
        currentUserService.findCurrentUser().ifPresent(user -> {
            model.addAttribute("displayName", user.getDisplayName());
            model.addAttribute("role", user.getRole().name());
        });
    }
}
