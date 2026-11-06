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

    /** ขนาดไฟล์ เช่น 1.2 MB */
    public String size(Long bytes) {
        if (bytes == null) return "";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.ROOT, "%.0f KB", bytes / 1024.0);
        return String.format(Locale.ROOT, "%.1f MB", bytes / 1024.0 / 1024.0);
    }
}
