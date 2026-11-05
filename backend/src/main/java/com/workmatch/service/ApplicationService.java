package com.workmatch.service;

import com.workmatch.dto.MatchView;
import com.workmatch.dto.Views.ApplicantItem;
import com.workmatch.dto.Views.CandidateDetail;
import com.workmatch.dto.Views.SkillCheck;
import com.workmatch.dto.Views.StudentApplicationItem;
import com.workmatch.entity.ApplicationStatus;
import com.workmatch.entity.Job;
import com.workmatch.entity.JobApplication;
import com.workmatch.entity.JobSkill;
import com.workmatch.entity.Resume;
import com.workmatch.entity.ResumeSkill;
import com.workmatch.entity.StudentProfile;
import com.workmatch.entity.User;
import com.workmatch.exception.JobNotFoundException;
import com.workmatch.repository.JobApplicationRepository;
import com.workmatch.repository.JobRepository;
import com.workmatch.repository.JobSkillRepository;
import com.workmatch.repository.ResumeRepository;
import com.workmatch.repository.ResumeSkillRepository;
import com.workmatch.repository.StoredFileRepository;
import com.workmatch.repository.StudentProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * การสมัครงาน (เอกสาร 1.4.1.2.5-6, 1.4.2.3): นักศึกษาสมัคร + ติดตามสถานะ (รูปที่ 42),
 * บริษัทดูผู้สมัคร แยกตามสถานะ (รูปที่ 45) ดูรายละเอียด (รูปที่ 46) อัปเดตสถานะ และส่งออก CSV
 */
@Service
public class ApplicationService {

    public static final int MAX_COVER_LETTER = 3000;

    private final JobApplicationRepository applications;
    private final JobRepository jobs;
    private final JobSkillRepository jobSkills;
    private final ResumeRepository resumes;
    private final ResumeSkillRepository resumeSkills;
    private final StudentProfileRepository studentProfiles;
    private final StoredFileRepository files;
    private final MatchService matchService;

    public ApplicationService(JobApplicationRepository applications, JobRepository jobs, JobSkillRepository jobSkills,
                              ResumeRepository resumes, ResumeSkillRepository resumeSkills,
                              StudentProfileRepository studentProfiles, StoredFileRepository files, MatchService matchService) {
        this.applications = applications;
        this.jobs = jobs;
        this.jobSkills = jobSkills;
        this.resumes = resumes;
        this.resumeSkills = resumeSkills;
        this.studentProfiles = studentProfiles;
        this.files = files;
        this.matchService = matchService;
    }

    // ---------- นักศึกษา ----------

    /** สมัครงาน: ต้องมีเรซูเม่ ประกาศต้องเปิดรับ และสมัครตำแหน่งเดิมซ้ำไม่ได้ */
    @Transactional
    public Long apply(User student, Long jobId, String coverLetter) {
        Job job = jobs.findById(jobId).orElseThrow(JobNotFoundException::new);
        if (!job.isOpen()) throw new IllegalArgumentException("ตำแหน่งนี้ปิดรับสมัครแล้ว");
        if (resumes.findByUserId(student.getId()).isEmpty()) {
            throw new IllegalArgumentException("กรุณาบันทึกเรซูเม่ก่อนสมัครงาน เพื่อให้บริษัทเห็นทักษะของคุณ");
        }
        if (applications.existsByStudentIdAndJobId(student.getId(), jobId)) {
            throw new IllegalArgumentException("คุณสมัครตำแหน่งนี้ไปแล้ว");
        }
        String letter = coverLetter == null ? "" : coverLetter.trim();
        if (letter.length() > MAX_COVER_LETTER) throw new IllegalArgumentException("ข้อความแนะนำตัวยาวเกิน 3,000 ตัวอักษร");
        JobApplication a = new JobApplication();
        a.setStudent(student);
        a.setJob(job);
        a.setCoverLetter(letter.isEmpty() ? null : letter);
        a.setStatus(ApplicationStatus.PENDING);
        return applications.save(a).getId();
    }

    @Transactional(readOnly = true)
    public List<StudentApplicationItem> forStudent(User student) {
        return applications.findForStudent(student.getId()).stream()
                .map(a -> new StudentApplicationItem(a.getId(), a.getJob().getId(), a.getJob().getTitle(),
                        a.getJob().getCompany().getDisplayName(), a.getAppliedAt(), a.getStatus()))
                .toList();
    }

    // ---------- บริษัท ----------

    /** ผู้สมัครของประกาศ (ตรวจว่าเป็นประกาศของบริษัทนี้) เรียงคะแนนมากไปน้อย */
    @Transactional(readOnly = true)
    public List<ApplicantItem> forJob(User company, Long jobId) {
        Job job = owned(company, jobId);
        return applications.findForJob(job.getId()).stream().map(a -> item(a, job))
                .sorted((x, y) -> Double.compare(nz(y.score()), nz(x.score())))
                .toList();
    }

    /** ผู้สมัครทุกตำแหน่งของบริษัท ล่าสุดก่อน (หน้า "ผู้สมัครทั้งหมด") */
    @Transactional(readOnly = true)
    public List<ApplicantItem> forCompany(User company, ApplicationStatus status) {
        return applications.findForCompany(company.getId()).stream()
                .filter(a -> status == null || a.getStatus() == status)
                .map(a -> item(a, a.getJob()))
                .toList();
    }

    public static Map<ApplicationStatus, Long> countByStatus(List<ApplicantItem> items) {
        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus s : ApplicationStatus.values()) counts.put(s, 0L);
        for (ApplicantItem i : items) counts.merge(i.status(), 1L, Long::sum);
        return counts;
    }

    @Transactional(readOnly = true)
    public CandidateDetail candidate(User company, Long applicationId) {
        JobApplication a = ownedApplication(company, applicationId);
        User s = a.getStudent();
        Optional<StudentProfile> p = studentProfiles.findByUserId(s.getId());
        Optional<Resume> resume = resumes.findByUserId(s.getId());
        Set<String> have = resume.map(r -> resumeSkills.findByResumeId(r.getId()).stream()
                .map(ResumeSkill::getSkillName).collect(Collectors.toCollection(java.util.LinkedHashSet::new)))
                .orElse(new java.util.LinkedHashSet<>());
        Set<String> haveLower = have.stream().map(x -> x.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        List<String> required = jobSkills.findByJobId(a.getJob().getId()).stream().map(JobSkill::getSkillName).toList();
        Set<String> requiredLower = required.stream().map(x -> x.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        List<SkillCheck> checks = required.stream().map(n -> new SkillCheck(n, haveLower.contains(n.toLowerCase(Locale.ROOT)))).toList();
        List<String> others = have.stream().filter(n -> !requiredLower.contains(n.toLowerCase(Locale.ROOT))).toList();
        Long fileId = resume.map(Resume::getResumeFileId).orElse(null);
        var file = fileId == null ? Optional.<com.workmatch.entity.StoredFile>empty() : files.findById(fileId);
        Double score = matchService.matchFor(s.getId(), a.getJob().getId()).map(MatchView::getScore).orElse(null);
        return new CandidateDetail(a.getId(), a.getJob().getId(), a.getJob().getTitle(), s.getDisplayName(),
                initials(s.getDisplayName()), s.getEmail(), p.map(StudentProfile::getPhone).orElse(null),
                p.map(StudentProfile::getStudentId).orElse(null), p.map(StudentProfile::getFaculty).orElse(null),
                p.map(StudentProfile::getMajor).orElse(null), a.getAppliedAt(), a.getStatus(), a.getCoverLetter(), score,
                checks, others, file.map(f -> f.getId()).orElse(null), file.map(f -> f.getFileName()).orElse(null),
                file.map(f -> f.getSizeBytes()).orElse(null));
    }

    @Transactional
    public Long updateStatus(User company, Long applicationId, ApplicationStatus status) {
        JobApplication a = ownedApplication(company, applicationId);
        a.setStatus(status);
        applications.save(a);
        return a.getJob().getId();
    }

    /** ไฟล์เรซูเม่ PDF ของผู้สมัคร (บริษัทเปิดได้เฉพาะของผู้ที่สมัครตำแหน่งของตัวเอง) */
    @Transactional(readOnly = true)
    public Optional<Long> resumeFileOf(User company, Long applicationId) {
        JobApplication a = ownedApplication(company, applicationId);
        return resumes.findByUserId(a.getStudent().getId()).map(Resume::getResumeFileId);
    }

    /** ส่งออกรายชื่อผู้สมัครเป็น CSV (UTF-8 พร้อม BOM เปิดใน Excel ภาษาไทยได้) ไม่มีรหัสผ่านหรือข้อมูลภายใน */
    @Transactional(readOnly = true)
    public String csv(User company, Long jobId) {
        List<ApplicantItem> items = forJob(company, jobId);
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        StringBuilder sb = new StringBuilder("﻿");
        sb.append("ลำดับ,ชื่อ-นามสกุล,รหัสนิสิต,สาขาวิชา,คะแนนความสอดคล้อง (%),ทักษะที่ตรง,สถานะ,วันที่สมัคร\r\n");
        int i = 1;
        for (ApplicantItem a : items) {
            sb.append(i++).append(',')
              .append(cell(a.studentName())).append(',')
              .append(cell(a.studentId())).append(',')
              .append(cell(a.major())).append(',')
              .append(a.score() == null ? "" : String.format(Locale.ROOT, "%.1f", a.score())).append(',')
              .append(cell(String.join(" | ", a.matchedSkills()))).append(',')
              .append(cell(a.status().getLabel())).append(',')
              .append(a.appliedAt() == null ? "" : a.appliedAt().format(f)).append("\r\n");
        }
        return sb.toString();
    }

    // ---------- ภายใน ----------

    private ApplicantItem item(JobApplication a, Job job) {
        User s = a.getStudent();
        Optional<StudentProfile> p = studentProfiles.findByUserId(s.getId());
        Optional<MatchView> m = matchService.matchFor(s.getId(), job.getId());
        return new ApplicantItem(a.getId(), job.getId(), job.getTitle(), s.getDisplayName(), initials(s.getDisplayName()),
                p.map(StudentProfile::getStudentId).orElse(null), p.map(StudentProfile::getMajor).orElse(null),
                m.map(MatchView::getMatched).orElse(List.of()), m.map(MatchView::getScore).orElse(null),
                a.getAppliedAt(), a.getStatus());
    }

    private Job owned(User company, Long jobId) {
        Job job = jobs.findById(jobId).orElseThrow(JobNotFoundException::new);
        if (!job.getCompany().getId().equals(company.getId())) throw new JobNotFoundException();
        return job;
    }

    private JobApplication ownedApplication(User company, Long applicationId) {
        JobApplication a = applications.findById(applicationId).orElseThrow(JobNotFoundException::new);
        if (!a.getJob().getCompany().getId().equals(company.getId())) throw new JobNotFoundException();
        return a;
    }

    /** อักษรย่อจากชื่อ-นามสกุล เช่น "ณัฐดนัย สิงห์งาม" -> "ณส" */
    public static String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(2, parts.length); i++) {
            String w = parts[i];
            int cp = w.codePointAt(0);
            if (cp >= 0x0E40 && cp <= 0x0E44 && w.length() > 1) cp = w.codePointAt(1);
            if (Character.isLetter(cp)) sb.appendCodePoint(Character.toUpperCase(cp));
        }
        return sb.length() == 0 ? "?" : sb.toString();
    }

    private static double nz(Double d) { return d == null ? -1 : d; }

    /** ป้องกัน CSV injection (ค่าขึ้นต้นด้วย = + - @) และครอบด้วย " */
    static String cell(String v) {
        if (v == null) return "";
        String s = v.replace("\r", " ").replace("\n", " ");
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
