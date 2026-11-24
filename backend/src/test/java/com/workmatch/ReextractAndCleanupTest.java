/*
 * ==== Workmatch · Backend · ทดสอบ (JUnit) ====
 * ทดสอบ: แผนวันที่ 30 พ.ย.–1 ธ.ค. (โมดูล 3)
 *   30 พ.ย. แก้พจนานุกรมแล้วทักษะในเรซูเม่เก่าอัปเดตตาม (คงทักษะที่นักศึกษาเพิ่มเอง ไม่ใส่คืนทักษะที่นักศึกษาลบเอง)
 *   1 ธ.ค. ลบไฟล์ PDF ที่ค้างในตาราง stored_files (ไม่ได้กดบันทึก และเก่ากว่า 24 ชั่วโมง)
 * รัน: cd backend แล้ว mvnw test (H2 + NLP จำลอง)
 */
package com.workmatch;

import com.workmatch.entity.Resume;
import com.workmatch.entity.ResumeSkill;
import com.workmatch.entity.StoredFile;
import com.workmatch.entity.User;
import com.workmatch.nlp.ExtractedSkill;
import com.workmatch.nlp.MatchResult;
import com.workmatch.nlp.NlpClient;
import com.workmatch.nlp.NlpUnavailableException;
import com.workmatch.repository.*;
import com.workmatch.service.DictionaryService;
import com.workmatch.service.RegistrationService;
import com.workmatch.service.StoredFileCleanupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ReextractAndCleanupTest {

    /** คำที่ NLP จำลอง "รู้จัก" ตอนนี้ (เปลี่ยนในเทสเพื่อจำลองการแก้พจนานุกรม) */
    private final List<String> known = new CopyOnWriteArrayList<>();

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired ResumeRepository resumes;
    @Autowired ResumeSkillRepository resumeSkills;
    @Autowired JobRepository jobs;
    @Autowired JobSkillRepository jobSkills;
    @Autowired JobScreeningCriteriaRepository criteria;
    @Autowired MatchRepository matches;
    @Autowired StoredFileRepository storedFiles;
    @Autowired SkillEntryRepository skillEntries;
    @Autowired SkillSynonymRepository synonyms;
    @Autowired DictionaryService dictionary;
    @Autowired RegistrationService registration;
    @Autowired StoredFileCleanupService cleanup;
    @Autowired JdbcTemplate jdbc;
    @MockBean NlpClient nlpClient;

    @BeforeEach
    void setUp() {
        matches.deleteAll();
        resumeSkills.deleteAll();
        resumes.deleteAll();
        criteria.deleteAll();
        jobSkills.deleteAll();
        jobs.deleteAll();
        storedFiles.deleteAll();
        users.deleteAll();
        synonyms.deleteAll();
        skillEntries.deleteAll();
        known.clear();
        known.addAll(List.of("Java", "Excel"));
        when(nlpClient.extractSkills(anyString())).thenAnswer(inv -> {
            String text = inv.getArgument(0, String.class).toLowerCase(Locale.ROOT);
            List<ExtractedSkill> out = new ArrayList<>();
            for (String s : known) if (text.contains(s.toLowerCase(Locale.ROOT))) out.add(new ExtractedSkill(s, "tools"));
            return out;
        });
        when(nlpClient.match(anyList(), anyList(), any())).thenAnswer(inv -> {
            List<String> have = inv.getArgument(0);
            List<String> need = inv.getArgument(1);
            List<String> matched = new ArrayList<>(), missing = new ArrayList<>();
            for (String s : need) (have.contains(s) ? matched : missing).add(s);
            double score = need.isEmpty() ? 0 : Math.round(matched.size() * 1000.0 / need.size()) / 10.0;
            return new MatchResult(score, score, matched, missing);
        });
        when(nlpClient.isHealthy()).thenReturn(true);
        dictionary.addSkill("Java", "programming");
        dictionary.addSkill("Excel", "office");
        dictionary.addSkill("SQL", "database");
        registration.createAdmin("ผู้ดูแล", "admin@test.com", "password123");
    }

    // ---------- 30 พ.ย. สกัดทักษะใหม่ทั้งหมดเมื่อแก้พจนานุกรม ----------

    @Test
    void editingDictionaryReextractsOldResumesButKeepsStudentEdits() throws Exception {
        registration.register("บริษัท ก", "a@co.com", "password123", "COMPANY");
        registration.registerStudent("นิสิต", "เก่า", "66000201", "old@test.com", "password123");
        // Docker มีในฐานข้อมูลพจนานุกรมแล้ว แต่ NLP ยังไม่รู้จัก (ยังไม่ได้ซิงก์) บริษัทเลือก Docker จากรายการเอง
        Long dockerId = dictionary.addSkill("Docker", "tools").getId();
        String job = mvc.perform(post("/company/jobs").with(csrf()).with(user("a@co.com").roles("COMPANY"))
                        .param("title", "DevOps Intern").param("description", "Java Docker").param("skills", "Docker")
                        .param("action", "publish"))
                .andReturn().getResponse().getRedirectedUrl();
        Long jobId = Long.valueOf(job.substring(job.lastIndexOf('/') + 1));

        mvc.perform(post("/student/resume").with(csrf()).with(user("old@test.com").roles("STUDENT"))
                        .param("text", "ใช้ Java และ Docker ทำงาน ใช้ Excel ทำรายงาน").param("action", "save"))
                .andExpect(redirectedUrl("/student/resume"));
        Resume r = resumeOf("old@test.com");
        assertThat(skills(r)).containsExactlyInAnyOrder("Java", "Excel");
        // นักศึกษาแก้เอง: ลบ Excel (สกัดผิดในมุมนักศึกษา) และเพิ่ม SQL เอง
        mvc.perform(post("/student/resume/skills/remove").with(csrf()).with(user("old@test.com").roles("STUDENT")).param("name", "Excel"));
        mvc.perform(post("/student/resume/skills/add").with(csrf()).with(user("old@test.com").roles("STUDENT")).param("name", "SQL"));
        assertThat(skills(r)).containsExactlyInAnyOrder("Java", "SQL");
        assertThat(matches.findByResumeIdAndJobId(r.getId(), jobId).orElseThrow().getScore()).isEqualByComparingTo("50.0");

        // แอดมินแก้ทักษะ Docker ในพจนานุกรม -> ส่งให้ NLP (รู้จัก Docker) -> เรซูเม่เก่าได้ Docker ทันที และคะแนนคำนวณใหม่
        known.add("Docker");
        mvc.perform(post("/admin/skills/" + dockerId).with(csrf()).with(user("admin@test.com").roles("ADMIN"))
                        .param("name", "Docker").param("category", "tools"))
                .andExpect(redirectedUrl("/admin/skills"))
                .andExpect(flash().attribute("message", org.hamcrest.Matchers.containsString("สกัดทักษะเรซูเม่ใหม่ 1 ฉบับ (ทักษะเปลี่ยน 1 ฉบับ")));
        assertThat(skills(r)).containsExactlyInAnyOrder("Java", "Docker", "SQL"); // Excel ไม่กลับมา, SQL ที่เพิ่มเองยังอยู่
        assertThat(resumeSkills.findByResumeId(r.getId()).stream().filter(ResumeSkill::isManual).map(ResumeSkill::getSkillName))
                .containsExactly("SQL");
        assertThat(matches.findByResumeIdAndJobId(r.getId(), jobId).orElseThrow().getScore()).isEqualByComparingTo("100.0");

        // ปุ่มสกัดใหม่ทั้งหมด: ไม่มีอะไรเปลี่ยน
        mvc.perform(post("/admin/skills/reextract").with(csrf()).with(user("admin@test.com").roles("ADMIN")))
                .andExpect(flash().attribute("message", "สกัดทักษะเรซูเม่ใหม่ 1 ฉบับ (ทักษะเปลี่ยน 0 ฉบับ)"));

        // นักศึกษาบันทึกข้อความใหม่ = เริ่มชุดทักษะใหม่ (รายการที่เคยลบถูกล้าง)
        mvc.perform(post("/student/resume").with(csrf()).with(user("old@test.com").roles("STUDENT"))
                        .param("text", "ใช้ Java และ Excel").param("action", "save"));
        assertThat(skills(resumeOf("old@test.com"))).containsExactlyInAnyOrder("Java", "Excel");
        assertThat(resumeOf("old@test.com").getRemovedSkillSet()).isEmpty();
    }

    @Test
    void nlpDownDuringReextractChangesNothing() throws Exception {
        registration.registerStudent("นิสิต", "ล่ม", "66000202", "down@test.com", "password123");
        mvc.perform(post("/student/resume").with(csrf()).with(user("down@test.com").roles("STUDENT"))
                .param("text", "Java Excel").param("action", "save"));
        Resume r = resumeOf("down@test.com");
        when(nlpClient.extractSkills(anyString())).thenThrow(new NlpUnavailableException());
        mvc.perform(post("/admin/skills/reextract").with(csrf()).with(user("admin@test.com").roles("ADMIN")))
                .andExpect(flash().attribute("message", org.hamcrest.Matchers.startsWith("ยังไม่ได้สกัดทักษะเรซูเม่ใหม่")));
        assertThat(skills(r)).containsExactlyInAnyOrder("Java", "Excel");
    }

    // ---------- 1 ธ.ค. ลบไฟล์ PDF ที่ค้าง ----------

    @Test
    void orphanPdfsOlderThanOneDayAreDeletedLinkedOrRecentOnesKept() throws Exception {
        User s = registration.registerStudent("นิสิต", "ไฟล์", "66000203", "file@test.com", "password123");
        mvc.perform(post("/student/resume").with(csrf()).with(user("file@test.com").roles("STUDENT"))
                .param("text", "Java").param("action", "save"));
        Long oldOrphan = file(s, 3);   // อัปโหลดแล้วไม่ได้บันทึก 3 วันก่อน -> ลบ
        Long recentOrphan = file(s, 0); // เพิ่งอัปโหลด อาจรอกดบันทึกอยู่ -> เก็บ
        Long linked = file(s, 5);       // ผูกกับเรซูเม่แล้ว -> เก็บ
        Resume r = resumeOf("file@test.com");
        r.setResumeFileId(linked);
        resumes.save(r);

        assertThat(cleanup.deleteOrphans(Duration.ofHours(24))).isEqualTo(1);
        assertThat(storedFiles.findById(oldOrphan)).isEmpty();
        assertThat(storedFiles.findById(recentOrphan)).isPresent();
        assertThat(storedFiles.findById(linked)).isPresent();

        // ปุ่มในหน้าภาพรวมผู้ดูแล
        file(s, 2);
        mvc.perform(post("/admin/files/cleanup").with(csrf()).with(user("admin@test.com").roles("ADMIN")))
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("message", "ลบไฟล์ PDF ที่ค้างแล้ว 1 ไฟล์"));
        mvc.perform(post("/admin/files/cleanup").with(csrf()).with(user("admin@test.com").roles("ADMIN")))
                .andExpect(flash().attribute("message", "ไม่มีไฟล์ PDF ที่ค้าง"));
    }

    private Long file(User owner, int daysAgo) {
        StoredFile f = new StoredFile();
        f.setOwner(owner);
        f.setFileName("cv.pdf");
        f.setContentType("application/pdf");
        f.setData(new byte[]{1, 2, 3});
        f.setSizeBytes(3);
        Long id = storedFiles.save(f).getId();
        jdbc.update("update stored_files set created_at = ? where id = ?", LocalDateTime.now().minusDays(daysAgo), id);
        return id;
    }

    private Resume resumeOf(String email) {
        return resumes.findByUserId(users.findByEmail(email).orElseThrow().getId()).orElseThrow();
    }

    private List<String> skills(Resume r) {
        return resumeSkills.findByResumeId(r.getId()).stream().map(ResumeSkill::getSkillName).toList();
    }
}
