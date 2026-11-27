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
import java.time.LocalDate;
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
    /** รูปแบบการทำงานที่เลือกได้ในฟอร์มประกาศงาน */
    public static final List<String> WORK_MODES = List.of("Onsite", "Hybrid", "Remote");

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
    private final com.workmatch.repository.JobScreeningCriteriaRepository criteria;
    private final ScreeningCriteriaService screening;

    public JobPostingService(JobRepository jobs, JobSkillRepository jobSkills, MatchRepository matches,
                             ResumeRepository resumes, JobApplicationRepository applications, BookmarkRepository bookmarks,
                             CompanyProfileRepository companyProfiles, NlpClient nlpClient, DictionaryService dictionary,
                             MatchService matchService, com.workmatch.repository.JobScreeningCriteriaRepository criteria,
                             ScreeningCriteriaService screening) {
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
        this.criteria = criteria;
        this.screening = screening;
    }

    // ---------- บริษัท: สร้าง / แก้ไข / ลบ ----------

    /** สร้างประกาศ: ทักษะ = ที่ NLP สกัดจากรายละเอียด + ที่เลือกจากพจนานุกรม แล้วจับคู่ใหม่ถ้าเผยแพร่ */
    @Transactional
    public Long create(User company, JobInput input) {
        return create(company, input, null);
    }

    /**
     * สร้างประกาศพร้อมเกณฑ์คัดกรองจากฟอร์มเดียวกัน (criteria = null คือไม่ได้ส่งส่วนเกณฑ์มา)
     * เกณฑ์ผิดข้อใด = โยน IllegalArgumentException แล้ว transaction ย้อนกลับ ประกาศก็ไม่ถูกบันทึก
     */
    @Transactional
    public Long create(User company, JobInput input, ScreeningCriteriaService.CriteriaInput criteria) {
        Job job = new Job();
        job.setCompany(company);
        apply(job, input);
        List<ExtractedSkill> skills = skillsFor(input);
        job = jobs.save(job);
        saveSkills(job, skills, java.util.Set.of());
        if (criteria != null) screening.save(company, job.getId(), criteria);
        return job.getId();
    }

    @Transactional
    public void update(User company, Long jobId, JobInput input) {
        update(company, jobId, input, null);
    }

    @Transactional
    public void update(User company, Long jobId, JobInput input, ScreeningCriteriaService.CriteriaInput criteria) {
        Job job = owned(company, jobId);
        apply(job, input);
        List<ExtractedSkill> skills = skillsFor(input);
        jobs.save(job);
        // แก้ประกาศ = สกัดทักษะใหม่ทั้งชุด แต่คงธงทักษะบังคับ (is_required) ของทักษะที่ยังอยู่ไว้
        java.util.Set<String> required = new java.util.HashSet<>();
        jobSkills.findByJobId(job.getId()).stream().filter(JobSkill::isRequired).forEach(s -> required.add(s.getSkillName()));
        jobSkills.deleteByJobId(job.getId());
        saveSkills(job, skills, required);
        if (criteria != null) screening.save(company, job.getId(), criteria);
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
        criteria.deleteByJobId(job.getId());
        jobSkills.deleteByJobId(job.getId());
        jobs.delete(job);
    }

    /** ค่าตั้งต้นของฟอร์มแก้ไข */
    @Transactional(readOnly = true)
    public JobInput inputFor(User company, Long jobId) {
        Job job = owned(company, jobId);
        List<String> selected = jobSkills.findByJobId(job.getId()).stream().map(JobSkill::getSkillName).toList();
        return new JobInput(job.getTitle(), job.getDescription(), nz(job.getCompensation()), nz(job.getWorkSchedule()),
                yesNo(job.getAccommodation()), yesNo(job.getTransport()), selected, job.getStatus() == JobStatus.DRAFT,
                nz(job.getLocation()), nz(job.getWorkMode()), job.getOpenings() == null ? "" : job.getOpenings().toString(),
                job.getDeadline() == null ? "" : job.getDeadline().toString());
    }

    // ---------- บริษัท: รายการและภาพรวม ----------

    @Transactional(readOnly = true)
    public List<CompanyJobRow> companyJobs(User company) {
        return jobs.findByCompanyId(company.getId()).stream()
                .sorted(Comparator.comparing(Job::getId).reversed())
                .map(j -> new CompanyJobRow(j.getId(), j.getTitle(), j.getCreatedAt(), j.getStatus(),
                        jobSkills.countByJobId(j.getId()),
                        applications.countByJobId(j.getId()), j.getDeadline(), j.isExpired()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CompanyOverview overview(User company) {
        List<CompanyJobRow> rows = companyJobs(company);
        long open = rows.stream().filter(r -> r.status() == JobStatus.OPEN && !r.expired()).count();
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
            if (j.isExpired()) continue;
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
                    j.getLocation() != null ? j.getLocation() : p == null ? null : shortAddress(p.getAddress()), p == null ? null : p.getBusinessType(),
                    p != null && p.isVerified(), skills, j.getCreatedAt(), days, score, saved.contains(j.getId()),
                    j.getWorkMode(), j.getDeadline()));
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
                resumes.findByUserId(student.getId()).isPresent(), j.getLocation(), j.getWorkMode(), j.getOpenings(),
                j.getDeadline(), j.isAccepting(), p.map(CompanyProfile::getAbout).orElse(null),
                p.map(CompanyProfile::getWebsite).orElse(null), screening.forJob(jobId));
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
        job.setAccommodation(choice(in.accommodation(), "สวัสดิการที่พัก"));
        job.setTransport(choice(in.transport(), "สวัสดิการรถรับส่ง"));
        job.setLocation(extra(in.location(), "สถานที่ปฏิบัติงาน"));
        String mode = nz(in.workMode()).trim();
        if (!mode.isEmpty() && !WORK_MODES.contains(mode)) throw new IllegalArgumentException("รูปแบบการทำงานไม่ถูกต้อง");
        job.setWorkMode(mode.isEmpty() ? null : mode);
        job.setOpenings(openings(in.openings()));
        LocalDate deadline = ProfileService.parseDate(in.deadline(), "วันปิดรับสมัคร");
        // ตั้งวันที่ผ่านมาแล้วไม่ได้ แต่แก้ประกาศเดิมที่หมดเขตแล้วโดยไม่เปลี่ยนวันได้
        if (deadline != null && deadline.isBefore(LocalDate.now()) && !deadline.equals(job.getDeadline())) {
            throw new IllegalArgumentException("วันปิดรับสมัครต้องไม่ใช่วันที่ผ่านมาแล้ว");
        }
        job.setDeadline(deadline);
        job.setStatus(in.draft() ? JobStatus.DRAFT : JobStatus.OPEN);
    }

    /**
     * ทักษะของประกาศ: NLP สกัดจากรายละเอียด (ล้มเหลวจะโยน NlpUnavailableException ก่อนบันทึก) + ที่เลือกจากพจนานุกรม
     * ถ้า skillsFinal (ช่องทักษะแบบป้ายในหน้าเว็บดึงทักษะจากรายละเอียดล่าสุดให้แล้ว) ใช้ชุดที่ผู้ใช้เลือกตามนั้น
     * ทักษะที่ผู้ใช้กด × ลบจะไม่ถูกอ่านกลับมาจากรายละเอียดงาน
     */
    private List<ExtractedSkill> skillsFor(JobInput in) {
        List<ExtractedSkill> all = in.skillsFinal() ? new ArrayList<>()
                : new ArrayList<>(nlpClient.extractSkills(in.description().trim()));
        if (in.selectedSkills() != null) {
            for (String name : in.selectedSkills()) {
                dictionary.resolve(name).map(SkillEntry.class::cast)
                        .ifPresent(s -> all.add(new ExtractedSkill(s.getName(), s.getCategory())));
            }
        }
        // ไม่พบทักษะก็บันทึกได้เหมือนเดิม (หน้าเว็บจะเตือนให้เพิ่มทักษะ) แต่ประกาศนั้นจะยังไม่ถูกจับคู่
        return ResumeService.dedupe(all);
    }

    /** ชื่อทักษะที่ NLP พบในรายละเอียดงาน ให้ดูก่อนบันทึก */
    public List<String> previewSkills(String description) {
        String d = nz(description).trim();
        if (d.isEmpty()) throw new IllegalArgumentException("กรุณากรอกรายละเอียดงาน");
        if (d.length() > JobService.MAX_DESCRIPTION) throw new IllegalArgumentException("รายละเอียดงานยาวเกิน 20,000 ตัวอักษร");
        return ResumeService.dedupe(nlpClient.extractSkills(d)).stream().map(ExtractedSkill::name).toList();
    }

    private void saveSkills(Job job, List<ExtractedSkill> skills, java.util.Set<String> required) {
        List<JobSkill> rows = new ArrayList<>();
        for (ExtractedSkill s : skills) {
            JobSkill row = new JobSkill(job, s.name(), s.category());
            row.setRequired(required.contains(s.name()));
            rows.add(row);
        }
        jobSkills.saveAll(rows);
    }

    private static Integer openings(String value) {
        String v = nz(value).trim();
        if (v.isEmpty()) return null;
        if (!v.matches("[0-9]{1,3}") || Integer.parseInt(v) < 1) {
            throw new IllegalArgumentException("จำนวนที่รับต้องเป็นตัวเลข 1-999");
        }
        return Integer.valueOf(v);
    }

    private static String extra(String value, String label) {
        String v = value == null ? "" : value.trim();
        if (v.length() > MAX_EXTRA) throw new IllegalArgumentException(label + "ยาวเกิน 255 ตัวอักษร");
        return v.isEmpty() ? null : v;
    }

    private static String nz(String s) { return s == null ? "" : s; }

    /** สวัสดิการที่พัก / รถรับส่ง: เลือกได้แค่ "มี" หรือ "ไม่มี" (ว่าง = ไม่ระบุ) */
    static final List<String> YES_NO = List.of("มี", "ไม่มี");

    private static String choice(String value, String label) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) return null;
        if (!YES_NO.contains(v)) throw new IllegalArgumentException("กรุณาเลือก" + label + "เป็น มี หรือ ไม่มี");
        return v;
    }

    /** ค่าเดิมที่เคยพิมพ์เป็นข้อความ แปลงเป็นตัวเลือก: ขึ้นต้นด้วย "ไม่มี" = ไม่มี, อย่างอื่น = มี */
    static String yesNo(String stored) {
        if (stored == null || stored.isBlank()) return "";
        String v = stored.trim();
        return v.startsWith("ไม่มี") ? "ไม่มี" : "มี";
    }

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
