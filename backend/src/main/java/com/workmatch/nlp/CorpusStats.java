/*
 * ==== Workmatch · Backend · เชื่อมต่อ NLP service (Python) ====
 * ไฟล์: backend/src/main/java/com/workmatch/nlp/CorpusStats.java
 * หน้าที่: สถิติคลังเอกสารที่ NLP ใช้คิดน้ำหนัก IDF ของ TF-IDF: docFreq = จำนวนเอกสารที่มีแต่ละทักษะ, totalDocs = N
 * เอกสาร 1 ฉบับ = ชุดทักษะของเรซูเม่ 1 ฉบับ หรือประกาศงานที่เปิดรับ 1 ประกาศ
 * สร้างโดย: MatchService.corpusStats() จากตาราง resume_skills และ job_skills
 * ส่งไปที่: NLP POST /match (ฟิลด์ doc_freq, total_docs) ผ่าน NlpClient
 */
package com.workmatch.nlp;

import java.util.Map;

public record CorpusStats(Map<String, Integer> docFreq, int totalDocs) {

    public static CorpusStats empty() {
        return new CorpusStats(Map.of(), 0);
    }
}
