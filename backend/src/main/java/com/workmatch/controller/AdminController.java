/*
 * ==== Workmatch · Backend · Controller (รับคำขอจากเบราว์เซอร์) ====
 * ไฟล์: backend/src/main/java/com/workmatch/controller/AdminController.java
 * หน้าที่: หน้าผู้ดูแลระบบ: ภาพรวม, อนุมัติบริษัท, พจนานุกรมทักษะ, คำพ้อง, บันทึกข้อผิดพลาด, จัดการผู้ใช้
 * URL ที่รับ: GET /admin, GET /admin/companies, POST /admin/companies/{userId}/review,
 *     GET /admin/companies/{userId}/document, GET /admin/skills, POST /admin/skills,
 *     POST /admin/skills/{id}, POST /admin/skills/{id}/toggle, GET /admin/synonyms, POST /admin/synonyms,
 *     POST /admin/synonyms/{id}/delete, GET /admin/logs, GET /admin/users, POST /admin/users/{id}/toggle
 * แสดงผลที่หน้า: frontend/templates/admin/companies.html, frontend/templates/admin/logs.html,
 *     frontend/templates/admin/overview.html, frontend/templates/admin/skills.html,
 *     frontend/templates/admin/synonyms.html, frontend/templates/admin/users.html
 * เรียกใช้: AdminService, DictionaryService, ErrorLogService, FileStorageService, CurrentUserService
 */
package com.workmatch.controller;

import com.workmatch.service.MatchService;
import com.workmatch.nlp.NlpUnavailableException;

import com.workmatch.service.AdminService;
import com.workmatch.service.CurrentUserService;
import com.workmatch.service.DictionaryService;
import com.workmatch.service.ErrorLogService;
import com.workmatch.service.FileStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * หน้าผู้ดูแลระบบ (เอกสาร 1.4.3, รูปที่ 48-52): ภาพรวมระบบ, อนุมัติสถานประกอบการ, พจนานุกรมทักษะ,
 * คำพ้องความหมาย, บันทึกข้อผิดพลาด และจัดการบัญชีผู้ใช้ (ระงับ/เปิดใช้งาน)
 */
@Controller
public class AdminController {

    private final AdminService adminService;
    private final DictionaryService dictionary;
    private final ErrorLogService errorLogs;
    private final FileStorageService fileStorage;
    private final CurrentUserService currentUserService;
    private final MatchService matchService;

    public AdminController(AdminService adminService, DictionaryService dictionary, ErrorLogService errorLogs,
                           FileStorageService fileStorage, CurrentUserService currentUserService, MatchService matchService) {
        this.matchService = matchService;
        this.adminService = adminService;
        this.dictionary = dictionary;
        this.errorLogs = errorLogs;
        this.fileStorage = fileStorage;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/admin")
    public String overview(Model model) {
        model.addAttribute("o", adminService.overview());
        model.addAttribute("section", "overview");
        return "admin/overview";
    }

    /** ปุ่ม "คำนวณคะแนนใหม่ทั้งหมด" (หน้าภาพรวม): คำนวณทุกคู่ด้วยสูตร TF-IDF + Cosine ปัจจุบัน */
    @PostMapping("/admin/matches/recalculate")
    public String recalculateMatches(RedirectAttributes redirect) {
        try {
            int n = matchService.recalculateAll();
            redirect.addFlashAttribute("message", "คำนวณคะแนนใหม่แล้ว " + n + " คู่");
        } catch (NlpUnavailableException e) {
            redirect.addFlashAttribute("error", "เชื่อมต่อ NLP ไม่ได้ ยังไม่ได้เปลี่ยนคะแนน ลองใหม่อีกครั้ง");
        }
        return "redirect:/admin";
    }

    // ---------- อนุมัติสถานประกอบการ ----------

    @GetMapping("/admin/companies")
    public String companies(Model model) {
        model.addAttribute("requests", adminService.verificationRequests());
        model.addAttribute("section", "companies");
        return "admin/companies";
    }

    @PostMapping("/admin/companies/{userId}/review")
    public String review(@PathVariable Long userId, @RequestParam String decision, RedirectAttributes redirect) {
        boolean approve = "approve".equals(decision);
        try {
            adminService.review(userId, approve, currentUserService.getCurrentUser());
            redirect.addFlashAttribute("message", approve ? "อนุมัติสถานประกอบการแล้ว" : "ไม่อนุมัติคำขอแล้ว");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/companies";
    }

    @GetMapping("/admin/companies/{userId}/document")
    public ResponseEntity<byte[]> document(@PathVariable Long userId) {
        return PdfResponses.inline(fileStorage.find(adminService.documentOf(userId)));
    }

    // ---------- พจนานุกรมทักษะ ----------

    @GetMapping("/admin/skills")
    public String skills(@RequestParam(required = false) String q, @RequestParam(required = false) Long edit, Model model) {
        model.addAttribute("skills", dictionary.search(q));
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("edit", edit);
        model.addAttribute("categories", DictionaryService.CATEGORIES);
        model.addAttribute("section", "skills");
        return "admin/skills";
    }

    @PostMapping("/admin/skills")
    public String addSkill(@RequestParam(required = false) String name, @RequestParam(required = false) String category,
                           RedirectAttributes redirect) {
        try {
            dictionary.addSkill(name, category);
            synced(redirect, "เพิ่มทักษะ " + name.trim() + " แล้ว");
        } catch (IllegalArgumentException | NullPointerException e) {
            redirect.addFlashAttribute("error", e instanceof NullPointerException ? "กรุณากรอกชื่อทักษะ" : e.getMessage());
        }
        return "redirect:/admin/skills";
    }

    @PostMapping("/admin/skills/{id}")
    public String updateSkill(@PathVariable Long id, @RequestParam(required = false) String name,
                              @RequestParam(required = false) String category, RedirectAttributes redirect) {
        try {
            dictionary.updateSkill(id, name, category);
            synced(redirect, "บันทึกการแก้ไขแล้ว");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/skills?edit=" + id;
        }
        return "redirect:/admin/skills";
    }

    @PostMapping("/admin/skills/{id}/toggle")
    public String toggleSkill(@PathVariable Long id, RedirectAttributes redirect) {
        boolean active = dictionary.toggleSkill(id);
        synced(redirect, active ? "เปิดใช้งานทักษะแล้ว" : "ปิดใช้งานทักษะแล้ว");
        return "redirect:/admin/skills";
    }

    // ---------- คำพ้องความหมาย ----------

    @GetMapping("/admin/synonyms")
    public String synonyms(Model model) {
        model.addAttribute("synonyms", dictionary.allSynonyms());
        model.addAttribute("skills", dictionary.activeSkills());
        model.addAttribute("section", "synonyms");
        return "admin/synonyms";
    }

    @PostMapping("/admin/synonyms")
    public String addSynonym(@RequestParam(required = false) String alias, @RequestParam(required = false) String target,
                             RedirectAttributes redirect) {
        try {
            dictionary.addSynonym(alias, target);
            synced(redirect, "เชื่อม \"" + alias.trim() + "\" กับ " + target + " แล้ว");
        } catch (IllegalArgumentException | NullPointerException e) {
            redirect.addFlashAttribute("error", e instanceof NullPointerException ? "กรุณากรอกคำศัพท์" : e.getMessage());
        }
        return "redirect:/admin/synonyms";
    }

    @PostMapping("/admin/synonyms/{id}/delete")
    public String deleteSynonym(@PathVariable Long id, RedirectAttributes redirect) {
        dictionary.deleteSynonym(id);
        synced(redirect, "ลบคำพ้องความหมายแล้ว");
        return "redirect:/admin/synonyms";
    }

    // ---------- ระบบ ----------

    @GetMapping("/admin/logs")
    public String logs(Model model) {
        model.addAttribute("logs", errorLogs.latest());
        model.addAttribute("section", "logs");
        return "admin/logs";
    }

    @GetMapping("/admin/users")
    public String users(Model model) {
        model.addAttribute("users", adminService.usersList());
        model.addAttribute("section", "users");
        return "admin/users";
    }

    @PostMapping("/admin/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            boolean enabled = adminService.toggleEnabled(id);
            redirect.addFlashAttribute("message", enabled ? "เปิดใช้งานบัญชีแล้ว" : "ระงับบัญชีแล้ว");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /** หลังแก้พจนานุกรม ส่งชุดใหม่ให้ NLP แล้วบอกผู้ดูแลว่าซิงก์สำเร็จหรือไม่ */
    private void synced(RedirectAttributes redirect, String message) {
        if (dictionary.pushToNlp()) {
            redirect.addFlashAttribute("message", message + " · ระบบ NLP ใช้ชุดใหม่แล้ว");
        } else {
            redirect.addFlashAttribute("message", message);
            redirect.addFlashAttribute("error", "บันทึกในฐานข้อมูลแล้ว แต่ส่งให้ระบบ NLP ไม่สำเร็จ (NLP อาจปิดอยู่) จะซิงก์อีกครั้งเมื่อเปิดระบบใหม่");
        }
    }
}
