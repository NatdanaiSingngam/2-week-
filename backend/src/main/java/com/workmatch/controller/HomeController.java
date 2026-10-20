package com.workmatch.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // หน้าแรกส่งไปหน้า login เสมอ
    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }
}
