package com.workmatch.nlp;

/** ทักษะ 1 ตัวที่ /extract ส่งกลับ เช่น {"name":"Java","category":"programming"} */
public record ExtractedSkill(String name, String category) {
}
