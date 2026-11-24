/*
 * ==== Workmatch · Backend · ทดสอบ (JUnit) ====
 * ทดสอบ: แผนวันที่ 23–27 พ.ย. (โมดูล 1–2)
 *   23 พ.ย. สาขาแบบเลือกจากรายการ (ค่าเดิมที่พิมพ์ไว้ไม่หาย)
 *   24 พ.ย. ลบเรซูเม่ทั้งฉบับ (ทักษะ ผลจับคู่ และไฟล์ PDF หายตาม ใบสมัครยังอยู่)
 *   25–26 พ.ย. เกณฑ์คัดกรอง 5 ข้อ + ทักษะบังคับ (JobScreeningCriteria, JobSkill.is_required) และฟอร์มของบริษัท
 *   27 พ.ย. หน้ารายละเอียดงานแสดงเกณฑ์ให้นักศึกษา
 * รัน: cd backend แล้ว mvnw test (H2 + NLP จำลอง ไม่ต้องเปิด MySQL/NLP)
 */
package com.workmatch;

import com.workmatch.entity.JobScreeningCriteria;
import com.workmatch.entity.JobSkill;
import com.workmatch.entity.Resume;
import com.workmatch.entity.StudentProfile;
import com.workmatch.entity.User;
import com.workmatch.nlp.ExtractedSkill;
import com.workmatch.nlp.MatchResult;
import com.workmatch.nlp.NlpClient;
import com.workmatch.repository.*;
import com.workmatch.service.DictionaryService;
import com.workmatch.service.RegistrationService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileResumeCriteriaTest {

    private static final List<String> KNOWN = List.of("Java", "Python", "SQL", "Docker", "Git");

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired ResumeRepository resumes;
    @Autowired ResumeSkillRepository resumeSkills;
    @Autowired JobRepository jobs;
    @Autowired JobSkillRepository jobSkills;
    @Autowired MatchRepository matches;
    @Autowired JobApplicationRepository applications;
    @Autowired StudentProfileRepository studentProfiles;
    @Autowired StoredFileRepository storedFiles;
    @Autowired JobScreeningCriteriaRepository criteria;
    @Autowired SkillEntryRepository skillEntries;
    @Autowired DictionaryService dictionary;
    @Autowired RegistrationService registration;
    @MockBean NlpClient nlpClient;

    @BeforeEach
    void setUp() {
        matches.deleteAll();
        resumeSkills.deleteAll();
        resumes.deleteAll();
        criteria.deleteAll();
        jobSkills.deleteAll();
        jobs.deleteAll();
        users.deleteAll();
        skillEntries.deleteAll();
        when(nlpClient.extractSkills(anyString())).thenAnswer(inv -> {
            String text = inv.getArgument(0, String.class).toLowerCase(Locale.ROOT);
            List<ExtractedSkill> out = new ArrayList<>();
            for (String s : KNOWN) if (text.contains(s.toLowerCase(Locale.ROOT))) out.add(new ExtractedSkill(s, "programming"));
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
        dictionary.addSkill("SQL", "database");
    }

    // ---------- 23 พ.ย. สาขาแบบเลือกจากรายการ ----------

    @Test
    void majorMustComeFromListButOldTypedValueIsKept() throws Exception {
        User s = registration.registerStudent("สมชาย", "ใจดี", "66000101", "major@test.com", "password123");
        // ค่านอกรายการ -> ไม่บันทึก แจ้งเตือน
        mvc.perform(post("/student/profile").with(csrf()).with(user("major@test.com").roles("STUDENT"))
                        .param("fullName", "สมชาย ใจดี").param("major", "ดาราศาสตร์"))
                .andExpect(view().name("student/profile"))
                .andExpect(model().attribute("error", "กรุณาเลือกสาขาวิชาจากรายการ"));
        assertThat(studentProfiles.findByUserId(s.getId()).orElseThrow().getMajor()).isNull();
        // ค่าในรายการ -> บันทึกได้
        mvc.perform(post("/student/profile").with(csrf()).with(user("major@test.com").roles("STUDENT"))
                        .param("fullName", "สมชาย ใจดี").param("major", "วิศวกรรมซอฟต์แวร์"))
                .andExpect(redirectedUrl("/student/profile"));
        assertThat(studentProfiles.findByUserId(s.getId()).orElseThrow().getMajor()).isEqualTo("วิศวกรรมซอฟต์แวร์");

        // ค่าเดิมที่เคยพิมพ์เอง (ก่อนเปลี่ยนเป็น dropdown) ยังแสดงเป็นตัวเลือกที่ถูกเลือก และบันทึกซ้ำได้
        StudentProfile p = studentProfiles.findByUserId(s.getId()).orElseThrow();
        p.setMajor("สาขาเก่าที่พิมพ์เอง");
        studentProfiles.save(p);
        String page = mvc.perform(get("/student/profile").with(user("major@test.com").roles("STUDENT")))
                .andReturn().getResponse().getContentAsString();
        assertThat(page).contains("<select id=\"major\"").contains("selected=\"selected\">สาขาเก่าที่พิมพ์เอง</option>");
        mvc.perform(post("/student/profile").with(csrf()).with(user("major@test.com").roles("STUDENT"))
                        .param("fullName", "สมชาย ใจดี").param("major", "สาขาเก่าที่พิมพ์เอง"))
                .andExpect(redirectedUrl("/student/profile"));
        assertThat(studentProfiles.findByUserId(s.getId()).orElseThrow().getMajor()).isEqualTo("สาขาเก่าที่พิมพ์เอง");
    }

    // ---------- 24 พ.ย. ลบเรซูเม่ทั้งฉบับ ----------

    @Test
    void deletingWholeResumeRemovesSkillsMatchesAndPdfButKeepsApplications() throws Exception {
        registration.register("บริษัท ก", "a@co.com", "password123", "COMPANY");
        registration.registerStudent("นิสิต", "ลบเรซูเม่", "66000102", "del@test.com", "password123");
        Long jobId = createJob("a@co.com", "Java Intern", "Java SQL");

        // อัปโหลด PDF แล้วกดบันทึก (ใช้ session เดียวกัน เพราะไฟล์รอบันทึกเก็บใน session)
        MockHttpSession session = new MockHttpSession();
        mvc.perform(multipart("/student/resume/upload")
                        .file(new MockMultipartFile("file", "cv.pdf", "application/pdf", pdf("Java SQL Git")))
                        .session(session).with(csrf()).with(user("del@test.com").roles("STUDENT")))
                .andExpect(status().isOk());
        mvc.perform(post("/student/resume").session(session).with(csrf()).with(user("del@test.com").roles("STUDENT"))
                        .param("text", "Java SQL Git").param("action", "save"))
                .andExpect(redirectedUrl("/student/resume"));
        Resume r = resumes.findByUserId(users.findByEmail("del@test.com").orElseThrow().getId()).orElseThrow();
        Long fileId = r.getResumeFileId();
        assertThat(fileId).isNotNull();
        assertThat(resumeSkills.findByResumeId(r.getId())).isNotEmpty();
        assertThat(matches.findByResumeId(r.getId())).isNotEmpty();
        mvc.perform(post("/student/jobs/" + jobId + "/apply").with(csrf()).with(user("del@test.com").roles("STUDENT")))
                .andExpect(redirectedUrl("/student/jobs/" + jobId));
        assertThat(applications.count()).isEqualTo(1);

        // หน้าเรซูเม่มีปุ่มลบทั้งฉบับพร้อมกล่องยืนยัน
        assertThat(mvc.perform(get("/student/resume").with(user("del@test.com").roles("STUDENT")))
                .andReturn().getResponse().getContentAsString())
                .contains("action=\"/student/resume/delete\"").contains("data-confirm=\"ลบเรซูเม่ทั้งฉบับ?");

        mvc.perform(post("/student/resume/delete").with(csrf()).with(user("del@test.com").roles("STUDENT")))
                .andExpect(redirectedUrl("/student/resume"))
                .andExpect(flash().attribute("message", "ลบเรซูเม่ทั้งฉบับแล้ว (ข้อความ ทักษะ ผลจับคู่ และไฟล์ PDF)"));
        assertThat(resumes.findById(r.getId())).isEmpty();
        assertThat(resumeSkills.findByResumeId(r.getId())).isEmpty();
        assertThat(matches.findByResumeId(r.getId())).isEmpty();
        assertThat(storedFiles.findById(fileId)).isEmpty();
        assertThat(applications.count()).isEqualTo(1); // ใบสมัครที่ส่งไปแล้วยังอยู่

        // ลบซ้ำตอนไม่มีเรซูเม่ ไม่พัง
        mvc.perform(post("/student/resume/delete").with(csrf()).with(user("del@test.com").roles("STUDENT")))
                .andExpect(flash().attribute("message", "ยังไม่มีเรซูเม่ให้ลบ"));
    }

    // ---------- 25–26 พ.ย. เกณฑ์คัดกรอง + ทักษะบังคับ ----------

    @Test
    void companySavesFiveCriteriaAndRequiredSkillsInOneTransaction() throws Exception {
        registration.register("บริษัท ก", "a@co.com", "password123", "COMPANY");
        registration.register("บริษัท ข", "b@co.com", "password123", "COMPANY");
        Long jobId = createJob("a@co.com", "Backend Intern", "Java SQL Docker");

        // เปิดฟอร์ม: เห็นทักษะของประกาศให้ติ๊ก
        String form = mvc.perform(get("/company/jobs/" + jobId + "/criteria").with(user("a@co.com").roles("COMPANY")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(form).contains("เกณฑ์คัดกรองผู้สมัคร").contains("value=\"Docker\"").contains("ทุกสาขา");

        mvc.perform(post("/company/jobs/" + jobId + "/criteria").with(csrf()).with(user("a@co.com").roles("COMPANY"))
                        .param("minMatchScore", "50").param("minGpa", "2.5").param("requiredMajor", "วิทยาการคอมพิวเตอร์")
                        .param("availableStart", "2027-06-01").param("availableEnd", "2027-09-30")
                        .param("requiredSkills", "Java", "SQL"))
                .andExpect(redirectedUrl("/company/jobs/" + jobId + "/criteria"));
        JobScreeningCriteria c = criteria.findByJobId(jobId).orElseThrow();
        assertThat(c.getMinMatchScore()).isEqualByComparingTo("50");
        assertThat(c.getMinGpa()).isEqualByComparingTo("2.50");
        assertThat(c.getRequiredMajor()).isEqualTo("วิทยาการคอมพิวเตอร์");
        assertThat(c.getAvailableStartDate()).isEqualTo(LocalDate.of(2027, 6, 1));
        assertThat(c.getAvailableEndDate()).isEqualTo(LocalDate.of(2027, 9, 30));
        assertThat(requiredSkills(jobId)).containsExactlyInAnyOrder("Java", "SQL");

        // อ่านกลับ: ฟอร์มแสดงค่าที่บันทึก และติ๊กทักษะบังคับไว้
        form = mvc.perform(get("/company/jobs/" + jobId + "/criteria").with(user("a@co.com").roles("COMPANY")))
                .andReturn().getResponse().getContentAsString();
        assertThat(form).contains("value=\"50\"").contains("value=\"2.50\"").contains("value=\"2027-06-01\"")
                .contains("value=\"Java\" checked=\"checked\"").doesNotContain("value=\"Docker\" checked");

        // แก้ประกาศ (สกัดทักษะใหม่) -> ธงบังคับของทักษะที่ยังอยู่คงเดิม
        mvc.perform(post("/company/jobs/" + jobId).with(csrf()).with(user("a@co.com").roles("COMPANY"))
                        .param("title", "Backend Intern").param("description", "Java Docker Git").param("action", "publish"))
                .andExpect(status().is3xxRedirection());
        assertThat(requiredSkills(jobId)).containsExactly("Java");

        // ล้างเกณฑ์ทั้งหมดได้ (เว้นว่างทุกข้อ)
        mvc.perform(post("/company/jobs/" + jobId + "/criteria").with(csrf()).with(user("a@co.com").roles("COMPANY")))
                .andExpect(redirectedUrl("/company/jobs/" + jobId + "/criteria"));
        assertThat(criteria.findByJobId(jobId).orElseThrow().isEmpty()).isTrue();
        assertThat(requiredSkills(jobId)).isEmpty();

        // บริษัทอื่นเปิด/แก้เกณฑ์ไม่ได้
        mvc.perform(get("/company/jobs/" + jobId + "/criteria").with(user("b@co.com").roles("COMPANY")))
                .andExpect(status().isNotFound());
        mvc.perform(post("/company/jobs/" + jobId + "/criteria").with(csrf()).with(user("b@co.com").roles("COMPANY"))
                        .param("minGpa", "3"))
                .andExpect(status().isNotFound());

        // ลบประกาศ -> เกณฑ์ถูกลบตาม
        mvc.perform(post("/company/jobs/" + jobId + "/delete").with(csrf()).with(user("a@co.com").roles("COMPANY")))
                .andExpect(status().is3xxRedirection());
        assertThat(criteria.findByJobId(jobId)).isEmpty();
    }

    @Test
    void outOfRangeCriteriaAreRejectedAndNothingIsSaved() throws Exception {
        registration.register("บริษัท ก", "a@co.com", "password123", "COMPANY");
        Long jobId = createJob("a@co.com", "Backend Intern", "Java SQL");
        String[][] bad = {
                {"minMatchScore", "120", "คะแนนความสอดคล้องขั้นต่ำต้องอยู่ระหว่าง 0–100 (ทศนิยมไม่เกิน 2 ตำแหน่ง)"},
                {"minMatchScore", "-1", "คะแนนความสอดคล้องขั้นต่ำต้องอยู่ระหว่าง 0–100 (ทศนิยมไม่เกิน 2 ตำแหน่ง)"},
                {"minGpa", "4.5", "GPA ขั้นต่ำต้องอยู่ระหว่าง 0.00–4.00 (ทศนิยมไม่เกิน 2 ตำแหน่ง)"},
                {"minGpa", "abc", "GPA ขั้นต่ำต้องอยู่ระหว่าง 0.00–4.00 (ทศนิยมไม่เกิน 2 ตำแหน่ง)"},
                {"requiredMajor", "ดาราศาสตร์", "กรุณาเลือกสาขาจากรายการ"},
                {"availableStart", "2027-06-01", "ช่วงสหกิจที่ต้องการต้องระบุทั้งวันเริ่มและวันสิ้นสุด"},
        };
        for (String[] b : bad) {
            mvc.perform(post("/company/jobs/" + jobId + "/criteria").with(csrf()).with(user("a@co.com").roles("COMPANY"))
                            .param(b[0], b[1]).param("requiredSkills", "Java"))
                    .andExpect(view().name("company/criteria"))
                    .andExpect(model().attribute("error", b[2]));
        }
        mvc.perform(post("/company/jobs/" + jobId + "/criteria").with(csrf()).with(user("a@co.com").roles("COMPANY"))
                        .param("availableStart", "2027-09-30").param("availableEnd", "2027-06-01"))
                .andExpect(model().attribute("error", "วันสิ้นสุดช่วงสหกิจต้องไม่ก่อนวันเริ่ม"));
        mvc.perform(post("/company/jobs/" + jobId + "/criteria").with(csrf()).with(user("a@co.com").roles("COMPANY"))
                        .param("requiredSkills", "Python"))
                .andExpect(model().attribute("error", "ทักษะบังคับต้องเป็นทักษะของประกาศนี้: Python"));
        // ไม่มีอะไรถูกบันทึกเลย (รวมทักษะบังคับที่ส่งมาพร้อมค่าผิด)
        assertThat(criteria.findByJobId(jobId)).isEmpty();
        assertThat(requiredSkills(jobId)).isEmpty();
    }

    // ---------- 27 พ.ย. หน้ารายละเอียดงานแสดงเกณฑ์ ----------

    @Test
    void studentSeesCriteriaAndRequiredSkillsBeforeApplying() throws Exception {
        registration.register("บริษัท ก", "a@co.com", "password123", "COMPANY");
        registration.registerStudent("นิสิต", "ดูเกณฑ์", "66000103", "view@test.com", "password123");
        Long jobId = createJob("a@co.com", "Backend Intern", "Java SQL Docker");

        String before = mvc.perform(get("/student/jobs/" + jobId).with(user("view@test.com").roles("STUDENT")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(before).contains("เกณฑ์คัดกรองของบริษัท").contains("บริษัทยังไม่ได้กำหนดเกณฑ์คัดกรองสำหรับตำแหน่งนี้");

        mvc.perform(post("/company/jobs/" + jobId + "/criteria").with(csrf()).with(user("a@co.com").roles("COMPANY"))
                        .param("minMatchScore", "60").param("minGpa", "2.75").param("requiredMajor", "วิศวกรรมซอฟต์แวร์")
                        .param("availableStart", "2027-06-01").param("availableEnd", "2027-09-30")
                        .param("requiredSkills", "Docker"))
                .andExpect(status().is3xxRedirection());
        String after = mvc.perform(get("/student/jobs/" + jobId).with(user("view@test.com").roles("STUDENT")))
                .andReturn().getResponse().getContentAsString();
        assertThat(after).contains("คะแนนความสอดคล้องขั้นต่ำ").contains("60.0%")
                .contains("GPA ขั้นต่ำ").contains("2.75")
                .contains("วิศวกรรมซอฟต์แวร์")
                .contains("ช่วงสหกิจที่ต้องการ")
                .contains("ทักษะบังคับ").contains("badge badge-required\">Docker<")
                .doesNotContain("บริษัทยังไม่ได้กำหนดเกณฑ์");
    }

    private List<String> requiredSkills(Long jobId) {
        return jobSkills.findByJobId(jobId).stream().filter(JobSkill::isRequired).map(JobSkill::getSkillName).toList();
    }

    private Long createJob(String company, String title, String description) throws Exception {
        String location = mvc.perform(post("/company/jobs").with(csrf()).with(user(company).roles("COMPANY"))
                        .param("title", title).param("description", description).param("action", "publish"))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        return Long.valueOf(location.substring(location.lastIndexOf('/') + 1));
    }

    private static byte[] pdf(String text) throws Exception {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(72, 700);
                cs.showText(text);
                cs.endText();
            }
            doc.save(out);
            return out.toByteArray();
        }
    }
}
