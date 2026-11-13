/*
 * ==== Workmatch · Backend · เชื่อมต่อ NLP service (Python) ====
 * ไฟล์: backend/src/main/java/com/workmatch/nlp/NlpClient.java
 * หน้าที่: ตัวเรียก NLP service (Python FastAPI ที่ http://localhost:8000) ทุกครั้งที่ backend ต้องสกัดทักษะหรือคิดคะแนน
 * ส่งไปที่: NLP service POST /extract, POST /match, GET/PUT /dictionary, GET /health (ดู nlp-service/api-contract.md)
 * ที่อยู่ NLP ตั้งใน backend/src/main/resources/application.properties (nlp.base-url)
 * ถูกเรียกโดย: AdminService, DictionaryService, JobPostingService, JobService, MatchService, ResumeService
 */
package com.workmatch.nlp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/** เรียก service NLP ของณัฐดนัย (FastAPI) ตาม nlp-service/api-contract.md */
@Component
public class NlpClient {

    private final RestClient restClient;

    public NlpClient(@Value("${nlp.base-url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000); // เชื่อมต่อไม่ได้ภายใน 2 วินาที = ล้มเหลว
        factory.setReadTimeout(5000);    // รอผลเกิน 5 วินาที = ล้มเหลว
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    /** POST /extract {"text": "..."} -> {"skills":[{"name","category"}]} */
    public List<ExtractedSkill> extractSkills(String text) {
        try {
            ExtractResponse body = restClient.post()
                    .uri("/extract")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", text))
                    .retrieve()
                    .body(ExtractResponse.class);
            if (body == null || body.skills() == null) return List.of();
            return body.skills();
        } catch (RestClientException e) {
            throw new NlpUnavailableException(e);
        }
    }

    /**
     * POST /match {"resume_skills":[...],"job_skills":[...],"doc_freq":{...},"total_docs":N} -> {score, cosine, matched, missing}
     * score = ค่าร้อยละความสอดคล้องแบบ TF-IDF + Cosine Similarity (คำนวณฝั่ง NLP เท่านั้น ห้ามคำนวณเองใน Java)
     * stats = สถิติคลังเอกสารสำหรับคิด IDF (CorpusStats.empty() = ทุกทักษะน้ำหนักเท่ากัน)
     */
    public MatchResult match(List<String> resumeSkills, List<String> jobSkills, CorpusStats stats) {
        try {
            Map<String, Object> request = new java.util.HashMap<>();
            request.put("resume_skills", resumeSkills);
            request.put("job_skills", jobSkills);
            if (stats != null && stats.totalDocs() > 0) {
                request.put("doc_freq", stats.docFreq());
                request.put("total_docs", stats.totalDocs());
            }
            MatchResult body = restClient.post()
                    .uri("/match")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(MatchResult.class);
            if (body == null) throw new NlpUnavailableException();
            return body;
        } catch (RestClientException e) {
            throw new NlpUnavailableException(e);
        }
    }

    /** GET /dictionary -> พจนานุกรมทักษะทั้งหมดของ NLP (ใช้นำเข้าครั้งแรกตอนตารางในฐานข้อมูลยังว่าง) */
    public List<DictionaryItem> fetchDictionary() {
        try {
            DictionaryBody body = restClient.get().uri("/dictionary").retrieve().body(DictionaryBody.class);
            return body == null || body.skills() == null ? List.of() : body.skills();
        } catch (RestClientException e) {
            throw new NlpUnavailableException(e);
        }
    }

    /** PUT /dictionary {"skills":[...]} แทนพจนานุกรมของ NLP ทั้งชุด (ผู้ดูแลระบบแก้ไขจากหน้าเว็บ) */
    public void replaceDictionary(List<DictionaryItem> skills) {
        try {
            restClient.put().uri("/dictionary")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new DictionaryBody(skills))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new NlpUnavailableException(e);
        }
    }

    /** GET /health ใช้แสดงสถานะระบบในหน้าผู้ดูแล ไม่โยน exception */
    public boolean isHealthy() {
        try {
            Map<?, ?> body = restClient.get().uri("/health").retrieve().body(Map.class);
            return body != null && "ok".equals(body.get("status"));
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** หนึ่งทักษะในพจนานุกรม: ชื่อมาตรฐาน หมวด และคำพ้องความหมาย */
    public record DictionaryItem(String name, String category, List<String> aliases) {
    }

    record DictionaryBody(List<DictionaryItem> skills) {
    }

    record ExtractResponse(List<ExtractedSkill> skills) {
    }
}
