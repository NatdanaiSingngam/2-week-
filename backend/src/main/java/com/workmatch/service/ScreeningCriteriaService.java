/*
 * ==== Workmatch · Backend · Service (ตรรกะหลักของระบบ) ====
 * ไฟล์: backend/src/main/java/com/workmatch/service/ScreeningCriteriaService.java
 * หน้าที่: กำหนด/อ่านเกณฑ์คัดกรองผู้สมัครของประกาศงาน (A04 UC-32, 1.4.2.3.3, ตาราง 47 และ 52)
 *          เกณฑ์ 5 ข้อ: Minimum Match Score, Required Skills, Minimum GPA, Major, Coop Availability Period
 * ถูกเรียกโดย: JobController (ฟอร์มเกณฑ์ของบริษัท), JobPostingService (หน้ารายละเอียดงานของนิสิต)
 * เรียกใช้: JobRepository, JobSkillRepository, JobScreeningCriteriaRepository, Majors, ProfileService.parseDate
 */
package com.workmatch.service;

import com.workmatch.entity.Job;
import com.workmatch.entity.JobScreeningCriteria;
import com.workmatch.entity.JobSkill;
import com.workmatch.entity.User;
import com.workmatch.exception.JobNotFoundException;
import com.workmatch.repository.JobRepository;
import com.workmatch.repository.JobScreeningCriteriaRepository;
import com.workmatch.repository.JobSkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * เกณฑ์เป็นแค่ "ตัวกำหนด" ยังไม่ตัดสินผู้สมัคร (เอนจินคัดกรองอยู่ในแผนวันที่ 2 ธ.ค.)
 * การบันทึกเกณฑ์ไม่เปลี่ยนคะแนนจับคู่หรือ Shortlist (Postconditions ของ UC-32)
 */
@Service
public class ScreeningCriteriaService {

    /** ค่าที่ผู้ใช้กรอกในฟอร์ม (ข้อความดิบ ยังไม่ตรวจ) ช่องว่าง = ไม่ใช้เกณฑ์ข้อนั้น */
    public record CriteriaInput(String minMatchScore, String minGpa, String requiredMajor,
                                String availableStart, String availableEnd, List<String> requiredSkills) {
        public static CriteriaInput empty() {
            return new CriteriaInput("", "", "", "", "", List.of());
        }
    }

    /** เกณฑ์ที่บันทึกแล้ว + ทักษะทั้งหมดของประกาศ (ให้ติ๊กบังคับ) */
    public record CriteriaView(Long jobId, String jobTitle, BigDecimal minMatchScore, BigDecimal minGpa,
                               String requiredMajor, LocalDate availableStart, LocalDate availableEnd,
                               List<String> jobSkills, List<String> requiredSkills) {
        /** ยังไม่ได้ตั้งเกณฑ์ข้อใดเลย (รวมทักษะบังคับ) */
        public boolean isEmpty() {
            return minMatchScore == null && minGpa == null && requiredMajor == null
                    && availableStart == null && availableEnd == null && requiredSkills.isEmpty();
        }

        /** ค่าตั้งต้นของฟอร์ม */
        public CriteriaInput toInput() {
            return new CriteriaInput(minMatchScore == null ? "" : minMatchScore.stripTrailingZeros().toPlainString(),
                    minGpa == null ? "" : minGpa.toPlainString(), requiredMajor == null ? "" : requiredMajor,
                    availableStart == null ? "" : availableStart.toString(),
                    availableEnd == null ? "" : availableEnd.toString(), requiredSkills);
        }
    }

    private final JobRepository jobs;
    private final JobSkillRepository jobSkills;
    private final JobScreeningCriteriaRepository criteria;

    public ScreeningCriteriaService(JobRepository jobs, JobSkillRepository jobSkills,
                                    JobScreeningCriteriaRepository criteria) {
        this.jobs = jobs;
        this.jobSkills = jobSkills;
        this.criteria = criteria;
    }

    /** เกณฑ์ของประกาศ สำหรับบริษัทเจ้าของประกาศเท่านั้น (ประกาศของบริษัทอื่น = 404) */
    @Transactional(readOnly = true)
    public CriteriaView forCompany(User company, Long jobId) {
        return view(owned(company, jobId));
    }

    /** เกณฑ์ของประกาศ สำหรับแสดงให้นิสิตเห็นก่อนสมัคร (หน้ารายละเอียดงาน) */
    @Transactional(readOnly = true)
    public CriteriaView forJob(Long jobId) {
        return view(jobs.findById(jobId).orElseThrow(JobNotFoundException::new));
    }

    /**
     * บันทึกเกณฑ์ (UC-32 Main Flow 3–4): ตรวจช่วงค่า แล้วบันทึก job_screening_criteria และ job_skills.is_required
     * ในธุรกรรมเดียวกัน ค่าผิดข้อใดข้อหนึ่ง = ไม่บันทึกอะไรเลย
     */
    @Transactional
    public void save(User company, Long jobId, CriteriaInput in) {
        Job job = owned(company, jobId);
        BigDecimal score = decimal(in.minMatchScore(), "100", 2, "คะแนนความสอดคล้องขั้นต่ำต้องอยู่ระหว่าง 0–100 (ทศนิยมไม่เกิน 2 ตำแหน่ง)");
        BigDecimal gpa = decimal(in.minGpa(), "4", 2, "GPA ขั้นต่ำต้องอยู่ระหว่าง 0.00–4.00 (ทศนิยมไม่เกิน 2 ตำแหน่ง)");
        String major = ProfileService.trim(in.requiredMajor());
        if (!major.isEmpty() && !Majors.isStandard(major)) throw new IllegalArgumentException("กรุณาเลือกสาขาจากรายการ");
        LocalDate start = ProfileService.parseDate(in.availableStart(), "วันเริ่มช่วงสหกิจที่ต้องการ");
        LocalDate end = ProfileService.parseDate(in.availableEnd(), "วันสิ้นสุดช่วงสหกิจที่ต้องการ");
        if ((start == null) != (end == null)) throw new IllegalArgumentException("ช่วงสหกิจที่ต้องการต้องระบุทั้งวันเริ่มและวันสิ้นสุด");
        if (start != null && end.isBefore(start)) throw new IllegalArgumentException("วันสิ้นสุดช่วงสหกิจต้องไม่ก่อนวันเริ่ม");

        List<JobSkill> skills = jobSkills.findByJobId(job.getId());
        Set<String> names = new HashSet<>();
        skills.forEach(s -> names.add(s.getSkillName()));
        Set<String> required = new HashSet<>(in.requiredSkills() == null ? List.of() : in.requiredSkills());
        for (String r : required) {
            if (!names.contains(r)) throw new IllegalArgumentException("ทักษะบังคับต้องเป็นทักษะของประกาศนี้: " + r);
        }

        JobScreeningCriteria c = criteria.findByJobId(job.getId()).orElseGet(() -> {
            JobScreeningCriteria n = new JobScreeningCriteria();
            n.setJob(job);
            return n;
        });
        c.setMinMatchScore(score);
        c.setMinGpa(gpa);
        c.setRequiredMajor(major.isEmpty() ? null : major);
        c.setAvailableStartDate(start);
        c.setAvailableEndDate(end);
        criteria.save(c);
        for (JobSkill s : skills) s.setRequired(required.contains(s.getSkillName()));
        jobSkills.saveAll(skills);
    }

    private CriteriaView view(Job job) {
        Optional<JobScreeningCriteria> c = criteria.findByJobId(job.getId());
        List<JobSkill> skills = jobSkills.findByJobId(job.getId());
        return new CriteriaView(job.getId(), job.getTitle(),
                c.map(JobScreeningCriteria::getMinMatchScore).orElse(null),
                c.map(JobScreeningCriteria::getMinGpa).orElse(null),
                c.map(JobScreeningCriteria::getRequiredMajor).orElse(null),
                c.map(JobScreeningCriteria::getAvailableStartDate).orElse(null),
                c.map(JobScreeningCriteria::getAvailableEndDate).orElse(null),
                skills.stream().map(JobSkill::getSkillName).toList(),
                skills.stream().filter(JobSkill::isRequired).map(JobSkill::getSkillName).toList());
    }

    private Job owned(User company, Long jobId) {
        Job job = jobs.findById(jobId).orElseThrow(JobNotFoundException::new);
        if (!job.getCompany().getId().equals(company.getId())) throw new JobNotFoundException();
        return job;
    }

    /** ตัวเลข 0..max ทศนิยมไม่เกิน scale ตำแหน่ง ช่องว่าง = null */
    private static BigDecimal decimal(String value, String max, int scale, String error) {
        String v = ProfileService.trim(value);
        if (v.isEmpty()) return null;
        BigDecimal d;
        try {
            d = new BigDecimal(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(error);
        }
        if (d.signum() < 0 || d.compareTo(new BigDecimal(max)) > 0 || Math.max(d.scale(), 0) > scale) {
            throw new IllegalArgumentException(error);
        }
        return d.setScale(scale);
    }
}
