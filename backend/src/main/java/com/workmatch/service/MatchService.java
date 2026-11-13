/*
 * ==== Workmatch · Backend · Service (ตรรกะหลักของระบบ) ====
 * ไฟล์: backend/src/main/java/com/workmatch/service/MatchService.java
 * หน้าที่: จับคู่เรซูเม่กับประกาศงาน: ส่งทักษะทั้งสองฝั่งให้ NLP คิดคะแนน แล้วเก็บผลลงตาราง matches
 * เรียกใช้: MatchRepository, JobRepository, JobSkillRepository, ResumeRepository, ResumeSkillRepository,
 *     NlpClient
 * ถูกเรียกโดย: ApplicationService, DemoDataLoader, JobController, JobPostingService, ResumeController,
 *     StudentDashboardController
 */
package com.workmatch.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workmatch.dto.ApplicantView;
import com.workmatch.dto.MatchView;
import com.workmatch.entity.Job;
import com.workmatch.entity.JobSkill;
import com.workmatch.entity.Match;
import com.workmatch.entity.Resume;
import com.workmatch.entity.ResumeSkill;
import com.workmatch.entity.User;
import com.workmatch.nlp.CorpusStats;
import com.workmatch.nlp.MatchResult;
import com.workmatch.nlp.NlpClient;
import com.workmatch.repository.JobRepository;
import com.workmatch.repository.JobSkillRepository;
import com.workmatch.repository.MatchRepository;
import com.workmatch.repository.ResumeRepository;
import com.workmatch.repository.ResumeSkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * งาน A4: จับคู่ resume กับประกาศงานผ่าน /match ของ NLP แล้วเก็บลงตาราง matches
 * คะแนนมาจาก NLP เท่านั้น Java แค่เก็บและเรียงลำดับ
 */
@Service
public class MatchService {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };

    private final MatchRepository matches;
    private final JobRepository jobs;
    private final JobSkillRepository jobSkills;
    private final ResumeRepository resumes;
    private final ResumeSkillRepository resumeSkills;
    private final NlpClient nlpClient;
    private final ObjectMapper objectMapper;

    /** ระบบแนะนำนักศึกษาให้บริษัทเมื่อคะแนนมากกว่าค่านี้ = ระดับ "ปานกลาง" ขึ้นไป (เกณฑ์ระดับอยู่ที่ MatchLevels) */
    public static final double RECOMMEND_MIN_SCORE = MatchLevels.UNSUITABLE_MAX;
    public static final BigDecimal RECOMMEND_MIN = BigDecimal.valueOf(RECOMMEND_MIN_SCORE);

    public MatchService(MatchRepository matches, JobRepository jobs, JobSkillRepository jobSkills,
                        ResumeRepository resumes, ResumeSkillRepository resumeSkills,
                        NlpClient nlpClient, ObjectMapper objectMapper) {
        this.matches = matches;
        this.jobs = jobs;
        this.jobSkills = jobSkills;
        this.resumes = resumes;
        this.resumeSkills = resumeSkills;
        this.nlpClient = nlpClient;
        this.objectMapper = objectMapper;
    }

    /** คำนวณ resume นี้กับทุกประกาศงาน (ข้ามประกาศที่ไม่มีทักษะ) คืนจำนวนคู่ที่คำนวณ */
    @Transactional
    public int recalculateForResume(Long resumeId) {
        Resume resume = resumes.findById(resumeId).orElseThrow();
        List<String> resumeSkillNames = resumeSkillNames(resume.getId());
        CorpusStats stats = corpusStats();
        int count = 0;
        for (Job job : jobs.findAll()) {
            if (!job.isOpen()) continue; // แบบร่าง / ปิดรับสมัคร ไม่นำมาจับคู่
            List<String> jobSkillNames = jobSkillNames(job.getId());
            if (jobSkillNames.isEmpty()) continue; // ประกาศที่ไม่มีทักษะ ข้ามและไม่สร้างแถว
            upsert(resume, job, nlpClient.match(resumeSkillNames, jobSkillNames, stats));
            count++;
        }
        return count;
    }

    /** คำนวณประกาศงานนี้กับทุก resume คืนจำนวนคู่ที่คำนวณ */
    @Transactional
    public int recalculateForJob(Long jobId) {
        Job job = jobs.findById(jobId).orElseThrow();
        if (!job.isOpen()) return 0;
        List<String> jobSkillNames = jobSkillNames(job.getId());
        if (jobSkillNames.isEmpty()) return 0;
        CorpusStats stats = corpusStats();
        int count = 0;
        for (Resume resume : resumes.findAll()) {
            upsert(resume, job, nlpClient.match(resumeSkillNames(resume.getId()), jobSkillNames, stats));
            count++;
        }
        return count;
    }

    /**
     * คำนวณคะแนนใหม่ทุกคู่ (ทุกเรซูเม่ x ทุกประกาศที่เปิดรับ) ใช้หลังเปลี่ยนสูตร หรือเมื่อคลังเอกสารเปลี่ยนจนน้ำหนัก IDF เปลี่ยน
     * อยู่ใน transaction เดียว ถ้า NLP ล้มกลางทางจะไม่มีคะแนนใดถูกเปลี่ยน คืนจำนวนคู่ที่คำนวณ
     */
    @Transactional
    public int recalculateAll() {
        int count = 0;
        for (Resume resume : resumes.findAll()) {
            count += recalculateForResume(resume.getId());
        }
        return count;
    }

    /** ข้อมูลหน้า Dashboard นักศึกษา เรียงคะแนนมากไปน้อย (ยังไม่มี resume = list ว่าง) */
    @Transactional(readOnly = true)
    public List<MatchView> dashboardFor(User student) {
        return resumes.findByUserId(student.getId())
                .map(resume -> matches.findByResumeId(resume.getId()).stream()
                        .filter(m -> m.getJob().isOpen())
                        .map(m -> new MatchView(
                                m.getJob().getId(),
                                m.getJob().getTitle(),
                                m.getJob().getCompany().getDisplayName(),
                                m.getScore().doubleValue(),
                                m.getCosine().doubleValue(),
                                fromJson(m.getMatchedSkills()),
                                fromJson(m.getMissingSkills())))
                        .sorted(Comparator.comparing(MatchView::getScore).reversed()
                                .thenComparing(MatchView::getJobId, Comparator.reverseOrder()))
                        .toList())
                .orElse(List.of());
    }

    /**
     * แท็บ "ระบบแนะนำ" ของบริษัท: นักศึกษาทุกคนที่มีเรซูเม่ (ไม่จำเป็นต้องสมัคร) ที่คะแนนกับประกาศนี้มากกว่า RECOMMEND_MIN_SCORE
     * เรียงคะแนนมากไปน้อย ไม่มีอีเมลหรือข้อมูลส่วนตัวอื่น
     */
    @Transactional(readOnly = true)
    public List<ApplicantView> applicantsFor(Long jobId) {
        return matches.findByJobId(jobId).stream()
                .filter(m -> m.getScore().compareTo(RECOMMEND_MIN) > 0)
                .map(m -> new ApplicantView(
                        m.getResume().getUser().getDisplayName(),
                        m.getScore().doubleValue(),
                        m.getCosine().doubleValue(),
                        fromJson(m.getMatchedSkills()),
                        fromJson(m.getMissingSkills())))
                .sorted(Comparator.comparing(ApplicantView::getScore).reversed())
                .toList();
    }

    /** ผลจับคู่ของนักศึกษาคนนี้กับประกาศนี้ (ยังไม่มีเรซูเม่หรือยังไม่ได้คำนวณ = empty) */
    @Transactional(readOnly = true)
    public java.util.Optional<MatchView> matchFor(Long studentUserId, Long jobId) {
        return resumes.findByUserId(studentUserId)
                .flatMap(r -> matches.findByResumeIdAndJobId(r.getId(), jobId))
                .map(m -> new MatchView(m.getJob().getId(), m.getJob().getTitle(),
                        m.getJob().getCompany().getDisplayName(), m.getScore().doubleValue(),
                        m.getCosine().doubleValue(), fromJson(m.getMatchedSkills()), fromJson(m.getMissingSkills())));
    }

    @Transactional(readOnly = true)
    public long applicantCount(Long jobId) {
        return matches.countByJobIdAndScoreGreaterThan(jobId, RECOMMEND_MIN);
    }

    // ---------- ภายใน ----------

    /** ถ้ามีแถวของคู่นี้แล้วให้อัปเดต ไม่สร้างแถวซ้ำ (unique resume_id + job_id) */
    private void upsert(Resume resume, Job job, MatchResult result) {
        Match row = matches.findByResumeIdAndJobId(resume.getId(), job.getId()).orElseGet(() -> {
            Match m = new Match();
            m.setResume(resume);
            m.setJob(job);
            return m;
        });
        row.setScore(oneDecimal(result.score()));
        row.setCosine(oneDecimal(result.cosine()));
        row.setMatchedSkills(toJson(result.matched()));
        row.setMissingSkills(toJson(result.missing()));
        matches.save(row);
    }

    /**
     * สถิติคลังเอกสารสำหรับ TF-IDF (เอกสาร 2.1, 2.3.3): เอกสาร 1 ฉบับ = ชุดทักษะของเรซูเม่ 1 ฉบับ
     * หรือประกาศงานที่เปิดรับและมีทักษะ 1 ประกาศ นับว่าแต่ละทักษะ (ตัวพิมพ์เล็ก) อยู่ในกี่เอกสาร
     */
    CorpusStats corpusStats() {
        Map<Long, Set<String>> byResume = new HashMap<>();
        for (ResumeSkill rs : resumeSkills.findAll()) {
            byResume.computeIfAbsent(rs.getResume().getId(), k -> new HashSet<>()).add(key(rs.getSkillName()));
        }
        Map<Long, Set<String>> byJob = new HashMap<>();
        for (JobSkill js : jobSkills.findAll()) {
            if (!js.getJob().isOpen()) continue;
            byJob.computeIfAbsent(js.getJob().getId(), k -> new HashSet<>()).add(key(js.getSkillName()));
        }
        Map<String, Integer> docFreq = new HashMap<>();
        for (Set<String> doc : byResume.values()) doc.forEach(k -> docFreq.merge(k, 1, Integer::sum));
        for (Set<String> doc : byJob.values()) doc.forEach(k -> docFreq.merge(k, 1, Integer::sum));
        docFreq.remove("");
        return new CorpusStats(docFreq, byResume.size() + byJob.size());
    }

    private static String key(String skill) {
        return skill == null ? "" : skill.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private List<String> resumeSkillNames(Long resumeId) {
        return resumeSkills.findByResumeId(resumeId).stream().map(ResumeSkill::getSkillName).toList();
    }

    private List<String> jobSkillNames(Long jobId) {
        return jobSkills.findByJobId(jobId).stream().map(JobSkill::getSkillName).toList();
    }

    private static BigDecimal oneDecimal(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
    }

    private String toJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list == null ? List.of() : list);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
