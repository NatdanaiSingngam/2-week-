/*
 * ==== Workmatch · Backend · Service (ตรรกะหลักของระบบ) ====
 * ไฟล์: backend/src/main/java/com/workmatch/service/JobPostingService.java
 * หน้าที่: ประกาศงานแบบเต็ม: สร้าง/แก้ไข/ลบ/แบบร่าง/เปลี่ยนสถานะ, ค้นหางาน, รายละเอียดงานฝั่งนักศึกษา
 * เรียกใช้: JobRepository, JobSkillRepository, MatchRepository, ResumeRepository, JobApplicationRepository,
 *     BookmarkRepository, CompanyProfileRepository, NlpClient, DictionaryService, MatchService
 * ถูกเรียกโดย: GlobalModelAdvice, JobController, StudentController, ThaiFormat
 */
package com.workmatch.service;

import com.workmatch.dto.MatchView;
import com.workmatch.dto.SkillView;
import com.workmatch.dto.Views.CompanyJobRow;
import com.workmatch.dto.Views.CompanyOverview;
import com.workmatch.dto.Views.ExploreItem;
import com.workmatch.dto.Views.JobInput;
import com.workmatch.dto.Views.StudentJobView;
import com.workmatch.entity.ApplicationStatus;
import com.workmatch.entity.CompanyProfile;
import com.workmatch.entity.Job;
import com.workmatch.entity.JobApplication;
import com.workmatch.entity.JobSkill;
import com.workmatch.entity.JobStatus;
import com.workmatch.entity.SkillEntry;
import com.workmatch.entity.User;
import com.workmatch.exception.JobNotFoundException;
import com.workmatch.nlp.ExtractedSkill;
import com.workmatch.nlp.NlpClient;
import com.workmatch.repository.BookmarkRepository;
import com.workmatch.repository.CompanyProfileRepository;
import com.workmatch.repository.JobApplicationRepository;
import com.workmatch.repository.JobRepository;
import com.workmatch.repository.JobSkillRepository;
import com.workmatch.repository.MatchRepository;
import com.workmatch.repository.ResumeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ประกาศงานแบบเต็มตามเอกสาร: สร้าง/แก้ไข/ลบ/แบบร่าง (รูปที่ 44), ภาพรวมบริษัท (รูปที่ 47),
 * ค้นหางาน (รูปที่ 38) และรายละเอียดงานฝั่งนักศึกษา (รูปที่ 39)
 * การสร้างแบบเดิม (ชื่อ + รายละเอียด) ยังอยู่ที่ JobService.createJob
 */
@Service
public class JobPostingService {

    private static final int MAX_EXTRA = 255;

    private final JobRepository jobs;
    private final JobSkillRepository jobSkills;
    private final MatchRepository matches;
    private final ResumeRepository resumes;
    private final JobApplicationRepository applications;
    private final BookmarkRepository bookmarks;
    private final CompanyProfileRepository companyProfiles;
    private final NlpClient nlpClient;
    private final DictionaryService dictionary;
    private final MatchService matchService;

    public JobPostingService(JobRepository jobs, JobSkillRepository jobSkills, MatchRepository matches,
                             ResumeRepository resumes, JobApplicationRepository applications, BookmarkRepository bookmarks,
                             CompanyProfileRepository companyProfiles, NlpClient nlpClient, DictionaryService dictionary,
                             MatchService matchService) {
        this.jobs = jobs;
        this.jobSkills = jobSkills;
        this.matches = matches;
        this.resumes = resumes;
        this.applications = applications;
        this.bookmarks = bookmarks;
        this.companyProfiles = companyProfiles;
        this.nlpClient = nlpClient;
        this.dictionary = dictionary;
        this.matchService = matchService;
    }

    // ---------- บริษัท: สร้าง / แก้ไข / ลบ ----------

    /** สร้างประกาศ: ทักษะ = ที่ NLP สกัดจากรายละเอียด + ที่เลือกจากพจนานุกรม แล้วจับคู่ใหม่ถ้าเผยแพร่ */
    @Transactional
    public Long create(User company, JobInput input) {
        Job job = new Job();
        job.setCompany(company);
        apply(job, input);
        List<ExtractedSkill> skills = skillsFor(input);
        job = jobs.save(job);
        saveSkills(job, skills);
        return job.getId();
    }

    @Transactional
    public void update(User company, Long jobId, JobInput input) {
        Job job = owned(company, jobId);
        apply(job, input);
        List<ExtractedSkill> skills = skillsFor(input);
        jobs.save(job);
        jobSkills.deleteByJobId(job.getId());
        saveSkills(job, skills);
        if (!job.isOpen()) matches.deleteByJobId(job.getId());
    }

    /** หลังบันทึก: เผยแพร่แล้วคำนวณคะแนนกับทุกเรซูเม่ (เรียกแยกนอก transaction เพื่อไม่ให้ NLP พังแล้วประกาศหาย) */
    public void recalculate(Long jobId) {
        matchService.recalculateForJob(jobId);
    }

    @Transactional
    public void setStatus(User company, Long jobId, JobStatus status) {
        Job job = owned(company, jobId);
        job.setStatus(status);
        jobs.save(job);
        if (status != JobStatus.OPEN) matches.deleteByJobId(jobId);
    }

    /** ลบประกาศ: ลบผลจับคู่และทักษะก่อน ใบสมัคร/รายการที่บันทึกถูกลบตาม (ON DELETE CASCADE) */
    @Transactional
    public void delete(User company, Long jobId) {
        Job job = owned(company, jobId);
        matches.deleteByJobId(job.getId());
        jobSkills.deleteByJobId(job.getId());
        jobs.delete(job);
    }

    /** ค่าตั้งต้นของฟอร์มแก้ไข */
    @Transactional(readOnly = true)
    public JobInput inputFor(User company, Long jobId) {
        Job job = owned(company, jobId);
        List<String> selected = jobSkills.findByJobId(job.getId()).stream().map(JobSkill::getSkillName).toList();
        return new JobInput(job.getTitle(), job.getDescription(), nz(job.getCompensation()), nz(job.getWorkSchedule()),
                nz(job.getAccommodation()), nz(job.getTransport()), selected, job.getStatus() == JobStatus.DRAFT);
    }

    // ---------- บริษัท: รายการและภาพรวม ----------

    @Transactional(readOnly = true)
    public List<CompanyJobRow> companyJobs(User company) {
        return jobs.findByCompanyId(company.getId()).stream()
                .sorted(Comparator.comparing(Job::getId).reversed())
                .map(j -> new CompanyJobRow(j.getId(), j.getTitle(), j.getCreatedAt(), j.getStatus(),
                        jobSkills.countByJobId(j.getId()),
                        matches.countByJobIdAndScoreGreaterThan(j.getId(), MatchService.RECOMMEND_MIN),
                        applications.countByJobId(j.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public CompanyOverview overview(User company) {
        List<CompanyJobRow> rows = companyJobs(company);
        long open = rows.stream().filter(r -> r.status() == JobStatus.OPEN).count();
        return new CompanyOverview(open,
                applications.countByJobCompanyId(company.getId()),
                applications.countByJobCompanyIdAndStatus(company.getId(), ApplicationStatus.PENDING),
                applications.countByJobCompanyIdAndStatus(company.getId(), ApplicationStatus.ACCEPTED),
                rows.stream().limit(5).toList());
    }

    // ---------- นักศึกษา: ค้นหางาน / รายละเอียด ----------

    /** ประกาศที่เปิดรับทั้งหมด ค้นจากชื่อตำแหน่ง ชื่อบริษัท หรือทักษะ และกรองประเภทกิจการ */
    @Transactional(readOnly = true)
    public List<ExploreItem> explore(User student, String query, String businessType) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String type = businessType == null ? "" : businessType.trim();
        Map<Long, CompanyProfile> profiles = companyProfiles.findAll().stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), Function.identity(), (a, b) -> a));
        Set<Long> saved = new HashSet<>(bookmarks.findJobIdsForStudent(student.getId()));
        Long resumeId = resumes.findByUserId(student.getId()).map(r -> r.getId()).orElse(null);
        List<ExploreItem> out = new ArrayList<>();
        for (Job j : jobs.findOpenJobs()) {
            CompanyProfile p = profiles.get(j.getCompany().getId());
            List<String> skills = jobSkills.findByJobId(j.getId()).stream().map(JobSkill::getSkillName).toList();
            if (!type.isEmpty() && (p == null || !type.equals(p.getBusinessType()))) continue;
            if (!q.isEmpty()) {
                boolean hit = j.getTitle().toLowerCase(Locale.ROOT).contains(q)
                        || j.getCompany().getDisplayName().toLowerCase(Locale.ROOT).contains(q)
                        || skills.stream().anyMatch(s -> s.toLowerCase(Locale.ROOT).contains(q));
                if (!hit) continue;
            }
            Double score = resumeId == null ? null : matches.findByResumeIdAndJobId(resumeId, j.getId())
                    .map(m -> m.getScore().doubleValue()).orElse(null);
            long days = Math.max(0, Duration.between(j.getCreatedAt(), LocalDateTime.now()).toDays());
            out.add(new ExploreItem(j.getId(), j.getTitle(), j.getCompany().getDisplayName(),
                    p == null ? null : shortAddress(p.getAddress()), p == null ? null : p.getBusinessType(),
                    p != null && p.isVerified(), skills, j.getCreatedAt(), days, score, saved.contains(j.getId())));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<String> businessTypes() {
        return companyProfiles.findBusinessTypes();
    }

    /** รายละเอียดงาน: นักศึกษาดูได้เฉพาะประกาศที่ไม่ใช่แบบร่าง (ปิดรับแล้วยังเปิดดูได้ แต่สมัครไม่ได้) */
    @Transactional(readOnly = true)
    public StudentJobView studentView(User student, Long jobId) {
        Job j = jobs.findById(jobId).orElseThrow(JobNotFoundException::new);
        if (j.getStatus() == JobStatus.DRAFT) throw new JobNotFoundException();
        Optional<CompanyProfile> p = companyProfiles.findByUserId(j.getCompany().getId());
        List<SkillView> skills = jobSkills.findByJobId(j.getId()).stream()
                .map(s -> new SkillView(s.getSkillName(), s.getCategory())).toList();
        MatchView match = matchService.matchFor(student.getId(), jobId).orElse(null);
        Optional<JobApplication> app = applications.findByStudentIdAndJobId(student.getId(), jobId);
        String name = j.getCompany().getDisplayName();
        return new StudentJobView(j.getId(), j.getTitle(), j.getDescription(), j.getStatus(), name, initial(name),
                p.map(CompanyProfile::isVerified).orElse(false),
                p.map(CompanyProfile::getAddress).orElse(null), p.map(CompanyProfile::getBusinessType).orElse(null),
                p.map(CompanyProfile::getEmployeeCount).orElse(null), j.getWorkSchedule(),
                p.map(CompanyProfile::getSelectionMethod).orElse(null), j.getCompensation(), j.getAccommodation(),
                j.getTransport(), skills, match, bookmarks.existsByStudentIdAndJobId(student.getId(), jobId),
                app.map(JobApplication::getStatus).orElse(null), app.map(JobApplication::getAppliedAt).orElse(null),
                resumes.findByUserId(student.getId()).isPresent());
    }

    // ---------- ภายใน ----------

    private Job owned(User company, Long jobId) {
        Job job = jobs.findById(jobId).orElseThrow(JobNotFoundException::new);
        if (!job.getCompany().getId().equals(company.getId())) throw new JobNotFoundException();
        return job;
    }

    private static void apply(Job job, JobInput in) {
        String t = in.title() == null ? "" : in.title().trim();
        String d = in.description() == null ? "" : in.description().trim();
        if (t.isEmpty()) throw new IllegalArgumentException("กรุณากรอกชื่อตำแหน่ง");
        if (t.length() > JobService.MAX_TITLE) throw new IllegalArgumentException("ชื่อตำแหน่งยาวเกิน 200 ตัวอักษร");
        if (d.isEmpty()) throw new IllegalArgumentException("กรุณากรอกรายละเอียดงาน");
        if (d.length() > JobService.MAX_DESCRIPTION) throw new IllegalArgumentException("รายละเอียดงานยาวเกิน 20,000 ตัวอักษร");
        job.setTitle(t);
        job.setDescription(d);
        job.setCompensation(extra(in.compensation(), "ค่าตอบแทน"));
        job.setWorkSchedule(extra(in.workSchedule(), "วันและเวลาทำงาน"));
        job.setAccommodation(extra(in.accommodation(), "สวัสดิการที่พัก"));
        job.setTransport(extra(in.transport(), "สวัสดิการรถรับส่ง"));
        job.setStatus(in.draft() ? JobStatus.DRAFT : JobStatus.OPEN);
    }

    /** ทักษะของประกาศ: NLP สกัดจากรายละเอียด (ล้มเหลวจะโยน NlpUnavailableException ก่อนบันทึก) + ที่เลือกจากพจนานุกรม */
    private List<ExtractedSkill> skillsFor(JobInput in) {
        List<ExtractedSkill> all = new ArrayList<>(nlpClient.extractSkills(in.description().trim()));
        if (in.selectedSkills() != null) {
            for (String name : in.selectedSkills()) {
                dictionary.resolve(name).map(SkillEntry.class::cast)
                        .ifPresent(s -> all.add(new ExtractedSkill(s.getName(), s.getCategory())));
            }
        }
        // ไม่พบทักษะก็บันทึกได้เหมือนเดิม (หน้าเว็บจะเตือนให้เพิ่มทักษะ) แต่ประกาศนั้นจะยังไม่ถูกจับคู่
        return ResumeService.dedupe(all);
    }

    private void saveSkills(Job job, List<ExtractedSkill> skills) {
        List<JobSkill> rows = new ArrayList<>();
        for (ExtractedSkill s : skills) rows.add(new JobSkill(job, s.name(), s.category()));
        jobSkills.saveAll(rows);
    }

    private static String extra(String value, String label) {
        String v = value == null ? "" : value.trim();
        if (v.length() > MAX_EXTRA) throw new IllegalArgumentException(label + "ยาวเกิน 255 ตัวอักษร");
        return v.isEmpty() ? null : v;
    }

    private static String nz(String s) { return s == null ? "" : s; }

    static String shortAddress(String address) {
        if (address == null || address.isBlank()) return null;
        String a = address.trim();
        for (String province : List.of("กรุงเทพ", "พะเยา", "เชียงใหม่", "เชียงราย", "ขอนแก่น", "ชลบุรี", "ภูเก็ต", "นนทบุรี", "ปทุมธานี")) {
            if (a.contains(province)) return province.equals("กรุงเทพ") ? "กรุงเทพมหานคร" : province;
        }
        return a.length() > 40 ? a.substring(0, 40) + "…" : a;
    }

    public static String initial(String name) {
        if (name == null || name.isBlank()) return "?";
        String n = name.trim().replaceFirst("^(บริษัท|บจก\\.?|ห้างหุ้นส่วน(จำกัด)?|หจก\\.?)\\s*", "");
        if (n.isEmpty()) n = name.trim();
        int cp = n.codePointAt(0);
        // สระหน้า (เ แ โ ใ ไ) อยู่ตัวเดียวอ่านไม่รู้เรื่อง จึงเอาพยัญชนะตัวถัดไปมาด้วย เช่น "เทค" -> "เท"
        if (cp >= 0x0E40 && cp <= 0x0E44 && n.length() > 1) return n.substring(0, 2);
        return new String(Character.toChars(cp)).toUpperCase(Locale.ROOT);
    }
}
