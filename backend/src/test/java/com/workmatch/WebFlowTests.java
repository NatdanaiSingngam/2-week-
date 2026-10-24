package com.workmatch;

import com.workmatch.entity.Job;
import com.workmatch.entity.Role;
import com.workmatch.entity.User;
import com.workmatch.nlp.ExtractedSkill;
import com.workmatch.nlp.NlpClient;
import com.workmatch.nlp.NlpUnavailableException;
import com.workmatch.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** ทดสอบเส้นทางจริงผ่าน Spring Security + Thymeleaf (ฐานข้อมูล H2, NLP จำลอง) ตามรายการตรวจ A2/A3 */
@SpringBootTest
@AutoConfigureMockMvc
class WebFlowTests {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired ResumeRepository resumes;
    @Autowired ResumeSkillRepository resumeSkills;
    @Autowired JobRepository jobs;
    @Autowired JobSkillRepository jobSkills;
    @Autowired MatchRepository matches;
    @Autowired PasswordEncoder encoder;
    @MockBean NlpClient nlpClient;

    @BeforeEach
    void clean() {
        matches.deleteAll();
        resumeSkills.deleteAll();
        resumes.deleteAll();
        jobSkills.deleteAll();
        jobs.deleteAll();
        users.deleteAll();
    }

    private User createUser(String email, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setDisplayName(email.substring(0, email.indexOf('@')));
        u.setPasswordHash(encoder.encode("password123"));
        u.setRole(role);
        return users.save(u);
    }

    // ---------- A2 ----------

    @Test
    void registerStudentAndCompanyStoresBcryptHash() throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("displayName", "สมชาย").param("email", "Somchai@Test.com")
                        .param("password", "password123").param("role", "STUDENT"))
                .andExpect(redirectedUrl("/login?registered"));
        mvc.perform(post("/register").with(csrf())
                        .param("displayName", "บริษัท A").param("email", "hr@a.com")
                        .param("password", "password123").param("role", "COMPANY"))
                .andExpect(redirectedUrl("/login?registered"));

        User s = users.findByEmail("somchai@test.com").orElseThrow();
        assertThat(s.getRole()).isEqualTo(Role.STUDENT);
        assertThat(s.getPasswordHash()).startsWith("$2").isNotEqualTo("password123");
        assertThat(users.findByEmail("hr@a.com").orElseThrow().getRole()).isEqualTo(Role.COMPANY);
    }

    @Test
    void duplicateEmailShowsErrorInsteadOfCrash() throws Exception {
        createUser("dup@test.com", Role.STUDENT);
        mvc.perform(post("/register").with(csrf())
                        .param("displayName", "x").param("email", "dup@test.com")
                        .param("password", "password123").param("role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("error", "อีเมลนี้ถูกใช้แล้ว"));
    }

    @Test
    void shortPasswordAndBadRoleRejected() throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("displayName", "x").param("email", "a@b.com")
                        .param("password", "1234567").param("role", "STUDENT"))
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));
        mvc.perform(post("/register").with(csrf())
                        .param("displayName", "x").param("email", "a@b.com")
                        .param("password", "password123").param("role", "ADMIN"))
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));
        assertThat(users.count()).isZero();
    }

    @Test
    void loginRedirectsByRoleAndWrongPasswordGoesToError() throws Exception {
        createUser("stu@test.com", Role.STUDENT);
        createUser("com@test.com", Role.COMPANY);
        mvc.perform(formLogin("/login").user("stu@test.com").password("password123"))
                .andExpect(redirectedUrl("/student/dashboard"));
        mvc.perform(formLogin("/login").user("com@test.com").password("password123"))
                .andExpect(redirectedUrl("/company/jobs"));
        mvc.perform(formLogin("/login").user("stu@test.com").password("wrong-pass"))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void accessRules() throws Exception {
        createUser("stu@test.com", Role.STUDENT);
        mvc.perform(get("/student/dashboard")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(get("/company/jobs").with(user("stu@test.com").roles("STUDENT")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(view().name("login"));
        mvc.perform(get("/register")).andExpect(status().isOk());
        mvc.perform(get("/css/style.css")).andExpect(status().isOk());
        mvc.perform(logout("/logout")).andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    void loggedInPagesGetDisplayNameAndRole() throws Exception {
        createUser("stu@test.com", Role.STUDENT);
        mvc.perform(get("/student/dashboard").with(user("stu@test.com").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("displayName", "stu"))
                .andExpect(model().attribute("role", "STUDENT"))
                .andExpect(model().attribute("hasResume", false));
    }

    // ---------- A3 ----------

    @Test
    void studentSavesResumeTwiceKeepsOneRowAndNewSkills() throws Exception {
        User s = createUser("stu@test.com", Role.STUDENT);
        when(nlpClient.extractSkills("มีความรู้ Python, Java และ SQL")).thenReturn(List.of(
                new ExtractedSkill("Python", "programming"), new ExtractedSkill("Java", "programming"),
                new ExtractedSkill("SQL", "database")));
        when(nlpClient.extractSkills("ใช้ Docker")).thenReturn(List.of(new ExtractedSkill("Docker", "devops")));

        mvc.perform(post("/student/resume").with(csrf()).with(user("stu@test.com").roles("STUDENT"))
                        .param("text", "มีความรู้ Python, Java และ SQL"))
                .andExpect(redirectedUrl("/student/resume"))
                .andExpect(flash().attribute("message", "บันทึกและวิเคราะห์ทักษะแล้ว"));
        Long resumeId = resumes.findByUserId(s.getId()).orElseThrow().getId();
        assertThat(resumeSkills.findByResumeId(resumeId)).hasSize(3);

        mvc.perform(post("/student/resume").with(csrf()).with(user("stu@test.com").roles("STUDENT"))
                        .param("text", "ใช้ Docker"))
                .andExpect(redirectedUrl("/student/resume"));
        assertThat(resumes.count()).isEqualTo(1);
        assertThat(resumeSkills.findByResumeId(resumeId)).extracting("skillName").containsExactly("Docker");

        mvc.perform(get("/student/resume").with(user("stu@test.com").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("resumeText", "ใช้ Docker"));
    }

    @Test
    void nlpDownShowsThaiErrorAndKeepsOldResume() throws Exception {
        User s = createUser("stu@test.com", Role.STUDENT);
        when(nlpClient.extractSkills("Java")).thenReturn(List.of(new ExtractedSkill("Java", "programming")));
        mvc.perform(post("/student/resume").with(csrf()).with(user("stu@test.com").roles("STUDENT")).param("text", "Java"));

        when(nlpClient.extractSkills("ข้อความใหม่")).thenThrow(new NlpUnavailableException());
        mvc.perform(post("/student/resume").with(csrf()).with(user("stu@test.com").roles("STUDENT"))
                        .param("text", "ข้อความใหม่"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/resume"))
                .andExpect(model().attribute("error", "ระบบวิเคราะห์ทักษะยังไม่พร้อม ลองใหม่อีกครั้ง"))
                .andExpect(model().attribute("resumeText", "ข้อความใหม่"));

        assertThat(resumes.findByUserId(s.getId()).orElseThrow().getRawText()).isEqualTo("Java");
        assertThat(resumeSkills.findByResumeId(resumes.findByUserId(s.getId()).orElseThrow().getId())).hasSize(1);
    }

    @Test
    void companyCreatesJobAndSeesItButOtherCompanyGets404() throws Exception {
        createUser("a@co.com", Role.COMPANY);
        createUser("b@co.com", Role.COMPANY);
        when(nlpClient.extractSkills(anyString())).thenReturn(List.of(
                new ExtractedSkill("Python", "programming"), new ExtractedSkill("SQL", "database"),
                new ExtractedSkill("Docker", "devops")));

        String location = mvc.perform(post("/company/jobs").with(csrf()).with(user("a@co.com").roles("COMPANY"))
                        .param("title", "Data Intern").param("description", "ต้องการ Python, SQL, Docker"))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
        assertThat(location).startsWith("/company/jobs/");
        Job job = jobs.findAll().get(0);
        assertThat(jobSkills.findByJobId(job.getId())).hasSize(3);

        mvc.perform(get("/company/jobs").with(user("a@co.com").roles("COMPANY")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("jobs", org.hamcrest.Matchers.hasSize(1)));
        mvc.perform(get(location).with(user("a@co.com").roles("COMPANY")))
                .andExpect(status().isOk())
                .andExpect(view().name("company/job-detail"));
        mvc.perform(get(location).with(user("b@co.com").roles("COMPANY")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/company/jobs/new").with(user("a@co.com").roles("COMPANY")))
                .andExpect(status().isOk())
                .andExpect(view().name("company/job-form"));
    }

    @Test
    void jobFormErrorKeepsInput() throws Exception {
        createUser("a@co.com", Role.COMPANY);
        mvc.perform(post("/company/jobs").with(csrf()).with(user("a@co.com").roles("COMPANY"))
                        .param("title", "").param("description", "Java"))
                .andExpect(status().isOk())
                .andExpect(view().name("company/job-form"))
                .andExpect(model().attributeExists("error"));
    }
}
