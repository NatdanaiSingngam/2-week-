/*
 * ==== Workmatch · Backend · เชื่อมต่อ NLP service (Python) ====
 * ไฟล์: backend/src/main/java/com/workmatch/nlp/MatchResult.java
 * หน้าที่: ผลคะแนนที่ NLP ตอบกลับจาก POST /match
 * ถูกเรียกโดย: MatchService, NlpClient
 */
package com.workmatch.nlp;

import java.util.List;

/** ผลจาก /match: {"score":50.0,"cosine":70.7,"matched":["Java"],"missing":["Git"]} */
public record MatchResult(double score, double cosine, List<String> matched, List<String> missing) {
}
