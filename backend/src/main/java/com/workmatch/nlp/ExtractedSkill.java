/*
 * ==== Workmatch · Backend · เชื่อมต่อ NLP service (Python) ====
 * ไฟล์: backend/src/main/java/com/workmatch/nlp/ExtractedSkill.java
 * หน้าที่: ทักษะ 1 ตัวที่ NLP ตอบกลับจาก POST /extract
 * ถูกเรียกโดย: JobPostingService, JobService, NlpClient, ResumeService
 */
package com.workmatch.nlp;

/** ทักษะ 1 ตัวที่ /extract ส่งกลับ เช่น {"name":"Java","category":"programming"} */
public record ExtractedSkill(String name, String category) {
}
