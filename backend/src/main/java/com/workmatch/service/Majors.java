/*
 * ==== Workmatch · Backend · Service (ตรรกะหลักของระบบ) ====
 * ไฟล์: backend/src/main/java/com/workmatch/service/Majors.java
 * หน้าที่: รายการสาขาวิชามาตรฐาน ใช้ร่วมกันระหว่าง Student.major (โปรไฟล์นิสิต) และ required_major (เกณฑ์คัดกรองของประกาศงาน)
 * ถูกเรียกโดย: ProfileService, StudentController, JobController, JobPostingService
 */
package com.workmatch.service;

import java.util.ArrayList;
import java.util.List;

/**
 * รายการสาขาวิชาให้เลือกจาก dropdown (A04 ข้อ 1.4.1.3.3, ตาราง 39 major และตาราง 52 required_major
 * "สาขาจากตัวเลือกมาตรฐานเดียวกับ Student.major")
 *
 * ⚠ รายการชั่วคราว: ทีมยังไม่ได้กำหนดรายการสาขาจริง (แผน 23 พ.ย. ให้ใช้รายการชั่วคราวไปก่อน)
 * แก้รายการที่นี่ที่เดียว ทั้งหน้าโปรไฟล์นิสิตและฟอร์มเกณฑ์ของบริษัทจะเปลี่ยนตาม
 */
public final class Majors {

    public static final List<String> ALL = List.of(
            "วิทยาการคอมพิวเตอร์",
            "วิศวกรรมคอมพิวเตอร์",
            "วิศวกรรมซอฟต์แวร์",
            "เทคโนโลยีสารสนเทศ",
            "วิทยาการข้อมูลและการประยุกต์",
            "ภูมิสารสนเทศศาสตร์",
            "คอมพิวเตอร์ธุรกิจ",
            "ธุรกิจดิจิทัล",
            "สื่อนฤมิต");

    private Majors() {
    }

    public static boolean isStandard(String major) {
        return major != null && ALL.contains(major.trim());
    }

    /**
     * ตัวเลือกที่แสดงใน dropdown: รายการมาตรฐาน + ค่าเดิมที่ผู้ใช้เคยพิมพ์ไว้ (ถ้าไม่อยู่ในรายการ)
     * เพื่อให้ "ค่าเดิมที่พิมพ์ไว้ไม่หาย" ตามเกณฑ์ในแผน
     */
    public static List<String> optionsWith(String current) {
        if (current == null || current.isBlank() || isStandard(current)) return ALL;
        List<String> options = new ArrayList<>(ALL);
        options.add(current.trim());
        return options;
    }
}
