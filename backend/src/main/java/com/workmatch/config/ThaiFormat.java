/*
 * ==== Workmatch · Backend · ตั้งค่าระบบ ====
 * ไฟล์: backend/src/main/java/com/workmatch/config/ThaiFormat.java
 * หน้าที่: ตัวช่วยจัดรูปแบบในหน้าเว็บ เรียกใน template ด้วย ${@fmt...} เช่น วันที่ พ.ศ. ขนาดไฟล์ ชื่อหมวดทักษะภาษาไทย
 * ใช้ในหน้า: admin/companies.html, admin/logs.html, admin/skills.html, admin/users.html,
 *     company/applications.html, company/candidate.html, company/job-detail.html, company/jobs.html,
 *     company/postings.html, student/applications.html, student/bookmarks.html, student/job.html,
 *     student/jobs.html, student/resume.html
 */
package com.workmatch.config;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.chrono.ThaiBuddhistDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * แสดงวันที่แบบไทย (พ.ศ.) ตามเอกสาร เช่น "31 ส.ค. 2569" เรียกใน Thymeleaf ด้วย ${@fmt.date(x)}
 */
@Component("fmt")
public class ThaiFormat {

    private static final Locale TH = Locale.forLanguageTag("th-TH");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", TH);

    public String date(LocalDateTime t) {
        return t == null ? "-" : DATE.format(ThaiBuddhistDate.from(t));
    }

    public String dateTime(LocalDateTime t) {
        return t == null ? "-" : DATE.format(ThaiBuddhistDate.from(t)) + " " + String.format("%02d:%02d", t.getHour(), t.getMinute());
    }

    /** "วันนี้" / "เมื่อวาน" / "3 วันที่แล้ว" */
    public String ago(LocalDateTime t) {
        if (t == null) return "-";
        long days = Math.max(0, Duration.between(t, LocalDateTime.now()).toDays());
        if (days == 0) return "วันนี้";
        if (days == 1) return "เมื่อวาน";
        return days + " วันที่แล้ว";
    }

    /** ตัวอักษรย่อของบริษัทในกล่องสี่เหลี่ยม (รูปที่ 38-41) เช่น "บริษัท สตรีม ไอที" -> "ส" */
    public String initial(String companyName) {
        return com.workmatch.service.JobPostingService.initial(companyName);
    }

    /** ชื่อไทยของหมวดทักษะ เช่น programming -> ภาษาโปรแกรม เรียกด้วย ${@fmt.category(code)} */
    public String category(String code) {
        return com.workmatch.service.SkillCategories.label(code);
    }

    /** หมวดทักษะที่มีอยู่ เรียงตามลำดับมาตรฐาน เรียกด้วย ${@fmt.categoryOrder(skills.![category])} */
    public java.util.List<String> categoryOrder(java.util.Collection<String> codes) {
        return com.workmatch.service.SkillCategories.ordered(codes);
    }

    /** ระดับของค่าร้อยละความสอดคล้อง: high / medium / low (เกณฑ์อยู่ที่ MatchLevels) เรียกด้วย ${@fmt.level(score)} */
    public String level(Number score) {
        return com.workmatch.service.MatchLevels.level(score);
    }

    /** ชื่อไทยของระดับ: เหมาะ / ปานกลาง / ไม่เหมาะ เรียกด้วย ${@fmt.levelLabel(lv)} */
    public String levelLabel(String level) {
        return com.workmatch.service.MatchLevels.label(level);
    }

    /** ขนาดไฟล์ เช่น 1.2 MB */
    public String size(Long bytes) {
        if (bytes == null) return "";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.ROOT, "%.0f KB", bytes / 1024.0);
        return String.format(Locale.ROOT, "%.1f MB", bytes / 1024.0 / 1024.0);
    }
}
