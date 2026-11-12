/*
 * ==== Workmatch · Backend · ทดสอบ (JUnit) ====
 * ทดสอบ: ฟีเจอร์ตามเอกสารบทที่ 3: ค้นหา/บันทึก/สมัครงาน, สถานะผู้สมัคร, admin, พจนานุกรม
 * รัน: cd backend แล้ว mvnw test (ใช้ฐานข้อมูล H2 และจำลอง NLP ด้วย Mockito ไม่ต้องเปิด MySQL/NLP)
 */
package com.workmatch;

import com.workmatch.entity.ApplicationStatus;
import com.workmatch.entity.CompanyProfile;
import com.workmatch.entity.JobApplication;
import com.workmatch.entity.Role;
import com.workmatch.entity.StudentProfile;
import com.workmatch.entity.User;
import com.workmatch.entity.VerificationStatus;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ฟีเจอร์ตามเอกสารบทที่ 3 (รูปที่ 34-52): ลงทะเบียนแบบเต็ม, ค้นหางาน, บันทึกงาน, สมัครงาน, ผู้สมัครแยกสถานะ,
 * CSV, รายละเอียดผู้สมัคร, แบบร่าง, โปรไฟล์ + ยืนยันตัวตน, อัปโหลด PDF, พจนานุกรม และหน้าผู้ดูแลระบบ
 * ใช้ H2 และ NLP จำลอง (สกัดทักษะจากคำที่อยู่ในข้อความ, คะแนน = ทักษะที่ตรง / ที่ต้องการ)
 */
@SpringBootTest
@AutoConfigureMockMvc
class DocumentFeaturesTest {

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
    @Autowired CompanyProfileRepository companyProfiles;
    @Autowired SkillEntryRepository skillEntries;
    @Autowired DictionaryService dictionary;
    @Autowired RegistrationService registration;
    @MockBean NlpClient nlpClient;

    @BeforeEach
    void setUp() {
        matches.deleteAll();
        resumeSkills.deleteAll();
        resumes.deleteAll();
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
        when(nlpClient.match(anyList(), anyList())).thenAnswer(inv -> {
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
        dictionary.addSkill("JavaScript", "programming");
    }

    private Long createJob(String company, String title, String description, String action) throws Exception {
        String location = mvc.perform(post("/company/jobs").with(csrf()).with(user(company).roles("COMPANY"))
                        .param("title", title).param("description", description)
                        .param("compensation", "300 บาท/วัน").param("action", action))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        return Long.valueOf(location.substring(location.lastIndexOf('/') + 1));
    }

    @Test
    void registerStudentAndCompanyWithDocumentFieldsCreatesProfiles() throws Exception {
        mvc.perform(post("/register").with(csrf()).param("role", "STUDENT").param("firstName", "ณัฐดนัย")
                        .param("lastName", "สิงห์งาม").param("studentId", "66123456").param("email", "nut@up.ac.th")
                        .param("password", "password123"))
                .andExpect(redirectedUrl("/login?registered"));
        User s = users.findByEmail("nut@up.ac.th").orElseThrow();
        assertThat(s.getDisplayName()).isEqualTo("ณัฐดนัย สิงห์งาม");
        assertThat(studentProfiles.findByUserId(s.getId()).orElseThrow().getStudentId()).isEqualTo("66123456");

        // รหัสนิสิตซ้ำ / รูปแบบผิด
        mvc.perform(post("/register").with(csrf()).param("role", "STUDENT").param("firstName", "ก").param("lastName", "ข")
                        .param("studentId", "66123456").param("email", "other@up.ac.th").param("password", "password123"))
                .andExpect(view().name("register")).andExpect(model().attribute("error", "รหัสนิสิตนี้ลงทะเบียนแล้ว"));
        mvc.perform(post("/register").with(csrf()).param("role", "STUDENT").param("firstName", "ก").param("lastName", "ข")
                        .param("studentId", "123").param("email", "x@up.ac.th").param("password", "password123"))
                .andExpect(view().name("register")).andExpect(model().attributeExists("error"));

        mvc.perform(post("/register").with(csrf()).param("role", "COMPANY").param("firstName", "สมศรี").param("lastName", "ใจดี")
                        .param("companyName", "บริษัท สตรีม จำกัด").param("taxId", "0105541000000").param("email", "hr@stream.co")
                        .param("password", "password123"))
                .andExpect(redirectedUrl("/login?registered"));
        User c = users.findByEmail("hr@stream.co").orElseThrow();
        CompanyProfile p = companyProfiles.findByUserId(c.getId()).orElseThrow();
        assertThat(c.getDisplayName()).isEqualTo("บริษัท สตรีม จำกัด");
        assertThat(p.getTaxId()).isEqualTo("0105541000000");
        assertThat(p.getVerificationStatus()).isEqualTo(VerificationStatus.NONE);
        mvc.perform(get("/register").param("role", "COMPANY")).andExpect(status().isOk());
    }

    @Test
    void studentExploresBookmarksAppliesAndCompanyManagesApplicants() throws Exception {
        registration.register("บริษัท A", "a@co.com", "password123", "COMPANY");
        User student = registration.register("นักศึกษา หนึ่ง", "s1@test.com", "password123", "STUDENT");
        Long jobId = createJob("a@co.com", "Backend Intern", "ต้องการ Java, SQL และ Docker", "publish");
        Long draftId = createJob("a@co.com", "Draft Intern", "ต้องการ Python", "draft");
        mvc.perform(post("/student/resume").with(csrf()).with(user("s1@test.com").roles("STUDENT")).param("text", "ใช้ Java และ SQL"));

        // ค้นหางาน: เห็นเฉพาะที่เผยแพร่ ไม่เห็นแบบร่าง
        String explore = mvc.perform(get("/student/jobs").with(user("s1@test.com").roles("STUDENT")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(explore).contains("Backend Intern").contains("Match 66.7%").doesNotContain("Draft Intern");
        assertThat(mvc.perform(get("/student/jobs").param("q", "python").with(user("s1@test.com").roles("STUDENT")))
                .andReturn().getResponse().getContentAsString()).doesNotContain("Backend Intern");
        mvc.perform(get("/student/jobs/" + draftId).with(user("s1@test.com").roles("STUDENT"))).andExpect(status().isNotFound());

        // รายละเอียดงาน + บันทึกงาน
        String detail = mvc.perform(get("/student/jobs/" + jobId).with(user("s1@test.com").roles("STUDENT")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(detail).contains("300 บาท/วัน").contains("Docker").contains("สมัครงาน");
        mvc.perform(post("/student/jobs/" + jobId + "/bookmark").with(csrf()).with(user("s1@test.com").roles("STUDENT"))
                        .param("back", "https://evil.example/"))
                .andExpect(redirectedUrl("/student/jobs/" + jobId));
        assertThat(mvc.perform(get("/student/bookmarks").with(user("s1@test.com").roles("STUDENT")))
                .andReturn().getResponse().getContentAsString()).contains("Backend Intern");

        // สมัครงาน ครั้งที่สองไม่ได้
        mvc.perform(post("/student/jobs/" + jobId + "/apply").with(csrf()).with(user("s1@test.com").roles("STUDENT"))
                        .param("coverLetter", "สนใจตำแหน่งนี้ครับ"))
                .andExpect(redirectedUrl("/student/jobs/" + jobId)).andExpect(flash().attributeExists("message"));
        mvc.perform(post("/student/jobs/" + jobId + "/apply").with(csrf()).with(user("s1@test.com").roles("STUDENT")))
                .andExpect(flash().attribute("error", "คุณสมัครตำแหน่งนี้ไปแล้ว"));
        assertThat(applications.count()).isEqualTo(1);
        assertThat(mvc.perform(get("/student/applications").with(user("s1@test.com").roles("STUDENT")))
                .andReturn().getResponse().getContentAsString()).contains("Backend Intern").contains("รอพิจารณา");

        // บริษัท: ภาพรวม, จัดการประกาศ, แท็บผู้สมัคร, CSV, รายละเอียด, อัปเดตสถานะ
        assertThat(mvc.perform(get("/company/jobs").with(user("a@co.com").roles("COMPANY")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).contains("Backend Intern").contains("รอการพิจารณา");
        assertThat(mvc.perform(get("/company/postings").with(user("a@co.com").roles("COMPANY")))
                .andReturn().getResponse().getContentAsString()).contains("แบบร่าง").contains("Draft Intern");
        String tab = mvc.perform(get("/company/jobs/" + jobId).with(user("a@co.com").roles("COMPANY")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(tab).contains("รอพิจารณา (1)").contains("นักศึกษา หนึ่ง").contains("67%"); // วงกลมแสดงจำนวนเต็ม (66.7 -> 67)
        String csv = mvc.perform(get("/company/jobs/" + jobId + "/applicants.csv").with(user("a@co.com").roles("COMPANY")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(csv).contains("\"นักศึกษา หนึ่ง\"").contains("66.7").contains("รอพิจารณา");

        Long appId = applications.findAll().get(0).getId();
        String candidate = mvc.perform(get("/company/applications/" + appId).with(user("a@co.com").roles("COMPANY")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(candidate).contains("สนใจตำแหน่งนี้ครับ").contains("Java (ตรงกัน)").contains("Docker (ยังไม่มี)");
        mvc.perform(post("/company/applications/" + appId + "/status").with(csrf()).with(user("a@co.com").roles("COMPANY"))
                        .param("status", "INTERVIEW"))
                .andExpect(redirectedUrl("/company/applications/" + appId));
        assertThat(applications.findById(appId).map(JobApplication::getStatus).orElseThrow()).isEqualTo(ApplicationStatus.INTERVIEW);
        assertThat(mvc.perform(get("/company/applications").param("status", "INTERVIEW").with(user("a@co.com").roles("COMPANY")))
                .andReturn().getResponse().getContentAsString()).contains("นักศึกษา หนึ่ง");

        // บริษัทอื่นเปิดใบสมัครนี้ไม่ได้, นักศึกษาเปิดหน้าบริษัทไม่ได้
        registration.register("บริษัท B", "b@co.com", "password123", "COMPANY");
        mvc.perform(get("/company/applications/" + appId).with(user("b@co.com").roles("COMPANY"))).andExpect(status().isNotFound());
        mvc.perform(get("/company/applications").with(user("s1@test.com").roles("STUDENT"))).andExpect(status().isForbidden());

        // แก้ไขเป็นแบบร่าง -> หายจากหน้าของนักศึกษา, ลบประกาศ -> ใบสมัครหายตาม
        mvc.perform(get("/company/jobs/" + jobId + "/edit").with(user("a@co.com").roles("COMPANY"))).andExpect(status().isOk());
        mvc.perform(post("/company/jobs/" + jobId + "/status").with(csrf()).with(user("a@co.com").roles("COMPANY")).param("status", "CLOSED"))
                .andExpect(redirectedUrl("/company/postings"));
        assertThat(mvc.perform(get("/student/dashboard").with(user("s1@test.com").roles("STUDENT")))
                .andReturn().getResponse().getContentAsString()).doesNotContain("Backend Intern");
        mvc.perform(post("/company/jobs/" + jobId + "/delete").with(csrf()).with(user("a@co.com").roles("COMPANY")))
                .andExpect(redirectedUrl("/company/postings"));
        assertThat(applications.count()).isZero();
        assertThat(student.getId()).isNotNull();
    }

    @Test
    void resumePdfUploadAndSkillEditing() throws Exception {
        registration.register("นักศึกษา PDF", "pdf@test.com", "password123", "STUDENT");
        MockMultipartFile pdf = new MockMultipartFile("file", "resume.pdf", "application/pdf", pdf("Skills: Java, Git and SQL"));
        mvc.perform(multipart("/student/resume/upload").file(pdf).with(csrf()).with(user("pdf@test.com").roles("STUDENT")))
                .andExpect(redirectedUrl("/student/resume")).andExpect(flash().attributeExists("message"));
        Long resumeId = resumes.findAll().get(0).getId();
        assertThat(resumeSkills.findByResumeId(resumeId)).extracting("skillName").containsExactly("Java", "SQL", "Git");
        assertThat(resumes.findById(resumeId).orElseThrow().getResumeFileId()).isNotNull();
        mvc.perform(get("/student/resume/file").with(user("pdf@test.com").roles("STUDENT")))
                .andExpect(status().isOk()).andExpect(content().contentType("application/pdf"));

        // ไฟล์ที่ไม่ใช่ PDF ถูกปฏิเสธ
        MockMultipartFile fake = new MockMultipartFile("file", "x.pdf", "application/pdf", "not a pdf".getBytes());
        mvc.perform(multipart("/student/resume/upload").file(fake).with(csrf()).with(user("pdf@test.com").roles("STUDENT")))
                .andExpect(flash().attribute("error", "รองรับเฉพาะไฟล์ PDF"));

        // ลบทักษะ / เพิ่มจากพจนานุกรม (คำพ้อง JS -> JavaScript) / คำนอกพจนานุกรมเพิ่มไม่ได้
        mvc.perform(post("/student/resume/skills/remove").with(csrf()).with(user("pdf@test.com").roles("STUDENT")).param("name", "Git"));
        dictionary.addSynonym("JS", "JavaScript");
        mvc.perform(post("/student/resume/skills/add").with(csrf()).with(user("pdf@test.com").roles("STUDENT")).param("name", "js"))
                .andExpect(flash().attribute("message", "เพิ่มทักษะแล้ว"));
        mvc.perform(post("/student/resume/skills/add").with(csrf()).with(user("pdf@test.com").roles("STUDENT")).param("name", "Cobol"))
                .andExpect(flash().attributeExists("error"));
        assertThat(resumeSkills.findByResumeId(resumeId)).extracting("skillName").containsExactly("Java", "SQL", "JavaScript");
        String page = mvc.perform(get("/student/resume").with(user("pdf@test.com").roles("STUDENT")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(page).contains("resume.pdf").contains("NLP Processing Complete");
    }

    @Test
    void studentProfileAndCompanyVerificationAndAdminPanel() throws Exception {
        User s = registration.registerStudent("สมชาย", "ใจดี", "66000099", "pf@test.com", "password123");
        mvc.perform(post("/student/profile").with(csrf()).with(user("pf@test.com").roles("STUDENT"))
                        .param("fullName", "สมชาย ใจดีมาก").param("phone", "081-234-5678")
                        .param("faculty", "เทคโนโลยีสารสนเทศ").param("major", "วิทยาการคอมพิวเตอร์").param("studentId", "11111111"))
                .andExpect(redirectedUrl("/student/profile"));
        StudentProfile sp = studentProfiles.findByUserId(s.getId()).orElseThrow();
        assertThat(sp.getStudentId()).isEqualTo("66000099"); // รหัสนิสิตแก้ไม่ได้
        assertThat(sp.getMajor()).isEqualTo("วิทยาการคอมพิวเตอร์");
        assertThat(users.findById(s.getId()).orElseThrow().getDisplayName()).isEqualTo("สมชาย ใจดีมาก");
        assertThat(mvc.perform(get("/student/dashboard").with(user("pf@test.com").roles("STUDENT")))
                .andReturn().getResponse().getContentAsString()).contains("66000099").contains("วิทยาการคอมพิวเตอร์");

        User c = registration.registerCompany("ผู้", "ติดต่อ", "บริษัท ตรวจสอบ จำกัด", "0105541000001", "verify@co.com", "password123");
        MockMultipartFile doc = new MockMultipartFile("document", "pp20.pdf", "application/pdf", pdf("VAT certificate"));
        mvc.perform(multipart("/company/profile").file(doc).with(csrf()).with(user("verify@co.com").roles("COMPANY"))
                        .param("companyName", "บริษัท ตรวจสอบ จำกัด").param("taxId", "0105541000001")
                        .param("businessType", "ไอที").param("address", "กรุงเทพมหานคร"))
                .andExpect(redirectedUrl("/company/profile"));
        assertThat(companyProfiles.findByUserId(c.getId()).orElseThrow().getVerificationStatus()).isEqualTo(VerificationStatus.PENDING);
        mvc.perform(get("/company/profile").with(user("verify@co.com").roles("COMPANY"))).andExpect(status().isOk());

        // ผู้ดูแลระบบ: เข้าหน้าได้ทุกหน้า อนุมัติบริษัท ระงับบัญชี
        registration.createAdmin("ผู้ดูแล", "admin@test.com", "password123");
        mvc.perform(formLogin("/login").user("admin@test.com").password("password123")).andExpect(redirectedUrl("/admin"));
        for (String url : List.of("/admin", "/admin/companies", "/admin/skills", "/admin/synonyms", "/admin/logs", "/admin/users")) {
            mvc.perform(get(url).with(user("admin@test.com").roles("ADMIN"))).andExpect(status().isOk());
        }
        mvc.perform(get("/admin").with(user("pf@test.com").roles("STUDENT"))).andExpect(status().isForbidden());
        assertThat(mvc.perform(get("/admin/companies").with(user("admin@test.com").roles("ADMIN")))
                .andReturn().getResponse().getContentAsString()).contains("บริษัท ตรวจสอบ จำกัด").contains("รออนุมัติ");
        mvc.perform(get("/admin/companies/" + c.getId() + "/document").with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().contentType("application/pdf"));
        mvc.perform(post("/admin/companies/" + c.getId() + "/review").with(csrf()).with(user("admin@test.com").roles("ADMIN"))
                        .param("decision", "approve"))
                .andExpect(redirectedUrl("/admin/companies"));
        assertThat(companyProfiles.findByUserId(c.getId()).orElseThrow().isVerified()).isTrue();
        assertThat(mvc.perform(get("/company/jobs").with(user("verify@co.com").roles("COMPANY")))
                .andReturn().getResponse().getContentAsString()).contains("ยืนยันตัวตนแล้ว");

        mvc.perform(post("/admin/users/" + s.getId() + "/toggle").with(csrf()).with(user("admin@test.com").roles("ADMIN")))
                .andExpect(redirectedUrl("/admin/users"));
        mvc.perform(formLogin("/login").user("pf@test.com").password("password123")).andExpect(redirectedUrl("/login?error"));
        Long adminId = users.findByEmail("admin@test.com").orElseThrow().getId();
        mvc.perform(post("/admin/users/" + adminId + "/toggle").with(csrf()).with(user("admin@test.com").roles("ADMIN")))
                .andExpect(flash().attribute("error", "ระงับบัญชีผู้ดูแลระบบไม่ได้"));
        assertThat(users.findById(adminId).orElseThrow().getRole()).isEqualTo(Role.ADMIN);

        // พจนานุกรม: เพิ่มทักษะ / คำพ้อง แล้วส่งให้ NLP
        mvc.perform(post("/admin/skills").with(csrf()).with(user("admin@test.com").roles("ADMIN"))
                        .param("name", "Kotlin").param("category", "programming"))
                .andExpect(redirectedUrl("/admin/skills"));
        mvc.perform(post("/admin/synonyms").with(csrf()).with(user("admin@test.com").roles("ADMIN"))
                        .param("alias", "kt").param("target", "Kotlin"))
                .andExpect(redirectedUrl("/admin/synonyms"));
        assertThat(mvc.perform(get("/admin/synonyms").with(user("admin@test.com").roles("ADMIN")))
                .andReturn().getResponse().getContentAsString()).contains("kt").contains("Kotlin");
        mvc.perform(post("/admin/skills").with(csrf()).with(user("admin@test.com").roles("ADMIN"))
                        .param("name", "kt").param("category", "programming"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void startupSyncAddsNewDictionaryEntriesFromNlpWithoutOverwritingAdminChanges() {
        // ผู้ดูแลปิดใช้งาน SQL ไว้ ส่วนทีม NLP เพิ่ม Kotlin และคำพ้อง "J2SE" ของ Java ในไฟล์ CSV
        Long sqlId = skillEntries.findByNameIgnoreCase("SQL").orElseThrow().getId();
        dictionary.toggleSkill(sqlId);
        when(nlpClient.fetchDictionary()).thenReturn(List.of(
                new NlpClient.DictionaryItem("Java", "programming", List.of("J2SE")),
                new NlpClient.DictionaryItem("SQL", "database", List.of()),
                new NlpClient.DictionaryItem("Kotlin", "programming", List.of("kt"))));

        dictionary.syncOnStartup();

        assertThat(skillEntries.findByNameIgnoreCase("Kotlin")).isPresent();
        assertThat(dictionary.resolve("kt").map(s -> s.getName())).contains("Kotlin");
        assertThat(dictionary.resolve("J2SE").map(s -> s.getName())).contains("Java");
        assertThat(skillEntries.findById(sqlId).orElseThrow().isActive()).isFalse(); // ไม่ทับการตั้งค่าของผู้ดูแล
        org.mockito.Mockito.verify(nlpClient, org.mockito.Mockito.atLeastOnce()).replaceDictionary(anyList());
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
