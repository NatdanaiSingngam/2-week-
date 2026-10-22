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
import java.util.List;

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
        int count = 0;
        for (Job job : jobs.findAll()) {
            List<String> jobSkillNames = jobSkillNames(job.getId());
            if (jobSkillNames.isEmpty()) continue; // ประกาศที่ไม่มีทักษะ ข้ามและไม่สร้างแถว
            upsert(resume, job, nlpClient.match(resumeSkillNames, jobSkillNames));
            count++;
        }
        return count;
    }

    /** คำนวณประกาศงานนี้กับทุก resume คืนจำนวนคู่ที่คำนวณ */
    @Transactional
    public int recalculateForJob(Long jobId) {
        Job job = jobs.findById(jobId).orElseThrow();
        List<String> jobSkillNames = jobSkillNames(job.getId());
        if (jobSkillNames.isEmpty()) return 0;
        int count = 0;
        for (Resume resume : resumes.findAll()) {
            upsert(resume, job, nlpClient.match(resumeSkillNames(resume.getId()), jobSkillNames));
            count++;
        }
        return count;
    }

    /** ข้อมูลหน้า Dashboard นักศึกษา เรียงคะแนนมากไปน้อย (ยังไม่มี resume = list ว่าง) */
    @Transactional(readOnly = true)
    public List<MatchView> dashboardFor(User student) {
        return resumes.findByUserId(student.getId())
                .map(resume -> matches.findByResumeId(resume.getId()).stream()
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

    /** ผู้สมัครของประกาศงาน (คะแนนมากกว่า 0) เรียงคะแนนมากไปน้อย ไม่มีอีเมลหรือข้อมูลส่วนตัวอื่น */
    @Transactional(readOnly = true)
    public List<ApplicantView> applicantsFor(Long jobId) {
        return matches.findByJobId(jobId).stream()
                .filter(m -> m.getScore().compareTo(BigDecimal.ZERO) > 0)
                .map(m -> new ApplicantView(
                        m.getResume().getUser().getDisplayName(),
                        m.getScore().doubleValue(),
                        m.getCosine().doubleValue(),
                        fromJson(m.getMatchedSkills()),
                        fromJson(m.getMissingSkills())))
                .sorted(Comparator.comparing(ApplicantView::getScore).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public long applicantCount(Long jobId) {
        return matches.countByJobIdAndScoreGreaterThan(jobId, BigDecimal.ZERO);
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
