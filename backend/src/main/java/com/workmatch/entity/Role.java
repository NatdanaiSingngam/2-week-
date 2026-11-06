package com.workmatch.entity;

/** บทบาทผู้ใช้ เก็บลงคอลัมน์ users.role เป็นข้อความ */
public enum Role {
    STUDENT,
    COMPANY,
    /** ผู้ดูแลระบบ (เอกสาร 1.4.3) สมัครผ่านหน้าเว็บไม่ได้ สร้างจาก profile demo หรือ WORKMATCH_ADMIN_EMAIL เท่านั้น */
    ADMIN
}
