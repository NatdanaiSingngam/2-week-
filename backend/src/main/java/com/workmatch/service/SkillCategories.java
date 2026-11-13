/*
 * ==== Workmatch · Backend · Service (ตรรกะหลักของระบบ) ====
 * ไฟล์: backend/src/main/java/com/workmatch/service/SkillCategories.java
 * หน้าที่: ลำดับและชื่อภาษาไทยของหมวดทักษะ (ที่เดียวที่ต้องแก้ ถ้าจะเพิ่ม/เปลี่ยนชื่อหมวด)
 * ถูกเรียกโดย: DictionaryService, ThaiFormat
 */
package com.workmatch.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * หมวดทักษะ: ฐานข้อมูลและ NLP เก็บเป็นคีย์อังกฤษ (คอลัมน์ category ใน skill_dictionary.csv)
 * ส่วนหน้าเว็บแสดงชื่อไทย ทั้งลำดับและชื่อไทยกำหนดไว้ที่นี่ที่เดียว ใช้ร่วมกันทุกหน้า
 *
 * ทำไมเก็บเป็นคีย์อังกฤษ แล้วแปลงเป็นไทยตอนแสดงผล (แยก "รหัส" ออกจาก "ข้อความที่ผู้ใช้เห็น")
 * 1. คีย์ไม่เปลี่ยน แต่ชื่อที่แสดงเปลี่ยนได้: จะแก้คำว่า "เครือข่ายและความปลอดภัย" เป็นคำอื่น แก้ที่ไฟล์นี้ไฟล์เดียว
 *    ไม่ต้องแก้ข้อมูลทุกแถวในฐานข้อมูล ไม่ต้องแก้ CSV และไม่ต้องแก้ NLP
 * 2. ใช้ร่วมกันหลายระบบ: Java, Python (NLP), ไฟล์ CSV และ JSON ใน API (nlp-service/api-contract.md) ใช้คีย์ชุดเดียวกัน
 *    คีย์อังกฤษสั้น ไม่มีช่องว่างหรือวรรณยุกต์ จึงไม่มีปัญหาการเข้ารหัสตัวอักษรหรือพิมพ์ต่างกันเล็กน้อย
 * 3. ตรวจความถูกต้องได้: ผู้ดูแลเลือกหมวดจากรายการคีย์ตายตัว (DictionaryService.CATEGORIES) พิมพ์ผิดแล้วระบบปฏิเสธ
 *    ถ้าเก็บเป็นข้อความไทย คำว่า "ฐานข้อมูล" กับ "ฐาน ข้อมูล" จะกลายเป็นคนละหมวด
 * 4. เพิ่มภาษาอื่นได้ภายหลัง: ถ้าจะทำหน้าเว็บภาษาอังกฤษ แค่เพิ่มชุดชื่อแสดงผลอีกชุด ข้อมูลเดิมใช้ได้ทั้งหมด
 * 5. ใช้เป็นค่าในโค้ดได้ตรงๆ: เช่น เทียบ/จัดกลุ่ม (skill.category == category) และเรียงลำดับ (ordered())
 */
public final class SkillCategories {

    /** ลำดับที่แสดงบนหน้าเว็บ = ลำดับใน map นี้ */
    private static final Map<String, String> LABELS = new LinkedHashMap<>();
    static {
        LABELS.put("programming", "ภาษาโปรแกรม");
        LABELS.put("frontend", "พัฒนาหน้าเว็บ (Frontend)");
        LABELS.put("backend", "พัฒนาระบบหลังบ้าน (Backend)");
        LABELS.put("mobile", "แอปพลิเคชันมือถือ");
        LABELS.put("database", "ฐานข้อมูล");
        LABELS.put("data", "ข้อมูลและ AI");
        LABELS.put("infrastructure", "เครือข่ายและความปลอดภัย");
        LABELS.put("testing", "การทดสอบซอฟต์แวร์");
        LABELS.put("tools", "เครื่องมือพัฒนา");
        LABELS.put("design", "ออกแบบ");
        LABELS.put("office", "โปรแกรมสำนักงาน");
        LABELS.put("language", "ภาษาต่างประเทศ");
        LABELS.put("soft", "ทักษะทางสังคม (Soft skills)");
    }

    /** หมวดที่ผู้ดูแลเลือกได้ ตามลำดับที่แสดง */
    public static final List<String> CODES = List.copyOf(LABELS.keySet());

    private SkillCategories() {
    }

    /** ชื่อไทยของหมวด: หมวดใหม่ที่ยังไม่มีชื่อไทยแสดงคีย์เดิม ไม่มีหมวด หรือ "other" แสดง "อื่น ๆ" */
    public static String label(String code) {
        if (code == null || code.isBlank() || code.equalsIgnoreCase("other")) return "อื่น ๆ";
        return LABELS.getOrDefault(code.trim().toLowerCase(), code);
    }

    /**
     * เรียงหมวดที่มีอยู่จริงตามลำดับมาตรฐาน ตัดตัวซ้ำ
     * หมวดที่ไม่รู้จักต่อท้ายเรียงตามตัวอักษร และหมวดว่าง (null) ไว้ท้ายสุด
     */
    public static List<String> ordered(Collection<String> codes) {
        Set<String> unique = new LinkedHashSet<>(codes == null ? List.of() : codes);
        List<String> result = new ArrayList<>(unique);
        result.sort(Comparator.comparingInt(SkillCategories::rank).thenComparing(c -> c == null ? "" : c));
        return result;
    }

    private static int rank(String code) {
        if (code == null) return Integer.MAX_VALUE;
        int i = CODES.indexOf(code);
        return i >= 0 ? i : CODES.size();
    }
}
