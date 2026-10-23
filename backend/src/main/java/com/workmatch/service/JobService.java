package com.workmatch.service;

import com.workmatch.dto.JobDetail;
import com.workmatch.dto.JobListItem;
import com.workmatch.dto.SkillView;
import com.workmatch.entity.Job;
import com.workmatch.entity.JobSkill;
import com.workmatch.entity.User;
import com.workmatch.exception.JobNotFoundException;
import com.workmatch.nlp.ExtractedSkill;
import com.workmatch.nlp.NlpClient;
import com.workmatch.repository.JobRepository;
import com.workmatch.repository.JobSkillRepository;
import com.workmatch.repository.MatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class JobService {

    public static final int MAX_TITLE = 200;
    public static final int MAX_DESCRIPTION = 20000;

    private final JobRepository jobs;
    private final JobSkillRepository jobSkills;
    private final NlpClient nlpClient;
    private final MatchRepository matches;

    public JobService(JobRepository jobs, JobSkillRepository jobSkills, NlpClient nlpClient, MatchRepository matches) {
        this.jobs = jobs;
        this.jobSkills = jobSkills;
        this.nlpClient = nlpClient;
        this.matches = matches;
    }

    /** สร้างประกาศงาน เรียก NLP กับ description แล้วบันทึก jobs + job_skills คืน id ของประกาศ */
    @Transactional
    public Long createJob(User company, String title, String description) {
        String t = title == null ? "" : title.trim();
        String d = description == null ? "" : description.trim();
        if (t.isEmpty()) throw new IllegalArgumentException("กรุณากรอกชื่อตำแหน่ง");
        if (t.length() > MAX_TITLE) throw new IllegalArgumentException("ชื่อตำแหน่งยาวเกิน 200 ตัวอักษร");
        if (d.isEmpty()) throw new IllegalArgumentException("กรุณากรอกรายละเอียดงาน");
        if (d.length() > MAX_DESCRIPTION) throw new IllegalArgumentException("รายละเอียดงานยาวเกิน 20,000 ตัวอักษร");

        List<ExtractedSkill> extracted = nlpClient.extractSkills(d); // ล้มเหลวจะไม่บันทึกอะไรเลย

        Job job = new Job();
        job.setCompany(company);
        job.setTitle(t);
        job.setDescription(d);
        job = jobs.save(job);

        List<JobSkill> rows = new ArrayList<>();
        for (ExtractedSkill s : ResumeService.dedupe(extracted)) {
            rows.add(new JobSkill(job, s.name(), s.category()));
        }
        jobSkills.saveAll(rows);
        return job.getId();
    }

    /** รายการประกาศของบริษัท ใหม่สุดอยู่บน applicantCount = จำนวนผู้ที่จับคู่ได้คะแนนมากกว่า 0 */
    @Transactional(readOnly = true)
    public List<JobListItem> listJobs(User company) {
        return jobs.findByCompanyId(company.getId()).stream()
                .sorted(Comparator.comparing(Job::getId).reversed())
                .map(j -> new JobListItem(j.getId(), j.getTitle(), jobSkills.countByJobId(j.getId()),
                        matches.countByJobIdAndScoreGreaterThan(j.getId(), BigDecimal.ZERO)))
                .toList();
    }

    /** บริษัทดูได้เฉพาะประกาศของตัวเอง ไม่ใช่ของตัวเองหรือไม่มีอยู่ -> JobNotFoundException (404) */
    @Transactional(readOnly = true)
    public JobDetail getJobDetail(User company, Long jobId) {
        Job job = jobs.findById(jobId).orElseThrow(JobNotFoundException::new);
        if (!job.getCompany().getId().equals(company.getId())) throw new JobNotFoundException();
        List<SkillView> skills = jobSkills.findByJobId(job.getId()).stream()
                .map(s -> new SkillView(s.getSkillName(), s.getCategory()))
                .toList();
        return new JobDetail(job.getId(), job.getTitle(), job.getDescription(), skills);
    }
}
