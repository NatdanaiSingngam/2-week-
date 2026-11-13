/*
 * ==== Workmatch · Backend · Service (ตรรกะหลักของระบบ) ====
 * ไฟล์: backend/src/main/java/com/workmatch/service/MatchLevels.java
 * หน้าที่: เกณฑ์ป้ายระดับของค่าร้อยละความสอดคล้อง (ที่เดียวที่ต้องแก้ ถ้าจะปรับเกณฑ์)
 *     0–40 = ไม่เหมาะ, มากกว่า 40 ถึงต่ำกว่า 90 = ปานกลาง, 90–100 = เหมาะ (ทีมกำหนด ไม่ได้มาจากเอกสาร)
 * ถูกเรียกโดย: ThaiFormat (${@fmt.level(score)}, ${@fmt.levelLabel(lv)} ในทุกหน้าที่แสดงคะแนน), MatchService (ขั้นต่ำของระบบแนะนำ)
 */
package com.workmatch.service;

public final class MatchLevels {

    /** ตั้งแต่ค่านี้ขึ้นไป = "เหมาะ" */
    public static final double SUITABLE_MIN = 90.0;
    /** ไม่เกินค่านี้ = "ไม่เหมาะ" มากกว่านี้ (แต่ต่ำกว่า SUITABLE_MIN) = "ปานกลาง" */
    public static final double UNSUITABLE_MAX = 40.0;

    private MatchLevels() {
    }

    /** คืนคีย์ระดับที่ใช้เป็น class CSS: high / medium / low (ไม่มีคะแนน = low) */
    public static String level(Number score) {
        if (score == null) return "low";
        double s = score.doubleValue();
        if (s >= SUITABLE_MIN) return "high";
        if (s > UNSUITABLE_MAX) return "medium";
        return "low";
    }

    /** ชื่อไทยของระดับ */
    public static String label(String level) {
        if ("high".equals(level)) return "เหมาะ";
        if ("medium".equals(level)) return "ปานกลาง";
        return "ไม่เหมาะ";
    }
}
