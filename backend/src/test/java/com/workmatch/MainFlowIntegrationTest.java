package com.workmatch;

import com.workmatch.dto.MatchView;
import com.workmatch.nlp.ExtractedSkill;
import com.workmatch.nlp.MatchResult;
import com.workmatch.nlp.NlpClient;
import com.workmatch.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * งาน A5: integration test เส้นทางหลักแบบผู้ใช้จริง (ใช้ session จาก login จริง ไม่ใช้ผู้ใช้จำลอง)
 * สมัคร -> login -> วาง resume -> บริษัทสร้างตำแหน่ง -> นักศึกษาเห็นคะแนน
 * NLP จำลองด้วย mock (ไม่ต้องรัน Python) ฐานข้อมูลเป็น H2
 */
@SpringBootTest
@AutoConfigureMockMvc
class MainFlowIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired ResumeRepository resumes;
    @Autowired ResumeSkillRepository resumeSkills;
    @Autowired JobRepository jobs;
    @Autowired JobSkillRepository jobSkills;
    @Autowired MatchRepository matches;
    @MockBean NlpClient nlpClient;

    @BeforeEach
    void setUp() {
        matches.deleteAll();
        resumeSkills.deleteAll();
        resumes.deleteAll();
        jobSkills.deleteAll();
        jobs.deleteAll();
        users.deleteAll();
        when(nlpClient.extractSkills(anyString())).thenAnswer(inv -> {
            String text = inv.getArgument(0);
            List<ExtractedSkill> out = new ArrayList<>();
            for (String s : List.of("Python", "SQL", "Docker")) if (text.contains(s)) out.add(new ExtractedSkill(s, "x"));
            return out;
        });
        when(nlpClient.match(anyList(), anyList())).thenAnswer(inv -> {
            List<String> have = inv.getArgument(0), need = inv.getArgument(1);
            List<String> matched = new ArrayList<>(), missing = new ArrayList<>();
            for (String s : need) (have.contains(s) ? matched : missing).add(s);
            double score = Math.round(matched.size() * 1000.0 / need.size()) / 10.0;
            return new MatchResult(score, score, matched, missing);
        });
    }

    private void register(String name, String email, String role) throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("displayName", name).param("email", email)
                        .param("password", "password123").param("role", role))
                .andExpect(redirectedUrl("/login?registered"));
    }

    private MockHttpSession login(String email, String expectedLanding) throws Exception {
        MvcResult r = mvc.perform(formLogin("/login").user(email).password("password123"))
                .andExpect(redirectedUrl(expectedLanding))
                .andReturn();
        return (MockHttpSession) r.getRequest().getSession(false);
    }

    @Test
    void registerLoginResumeJobThenStudentSeesScore() throws Exception {
        // 1) สมัครทั้งสองฝั่ง
        register("นักศึกษา เส้นทางหลัก", "main-student@test.com", "STUDENT");
        register("บริษัท เส้นทางหลัก", "main-company@test.com", "COMPANY");

        // 2) นักศึกษา login แล้ววาง resume
        MockHttpSession student = login("main-student@test.com", "/student/dashboard");
        mvc.perform(get("/student/dashboard").session(student))
                .andExpect(status().isOk())
                .andExpect(model().attribute("hasResume", false));
        mvc.perform(post("/student/resume").session(student).with(csrf())
                        .param("text", "มีความรู้ Python และ SQL"))
                .andExpect(redirectedUrl("/student/resume"));

        // 3) บริษัท login แล้วสร้างตำแหน่ง
        MockHttpSession company = login("main-company@test.com", "/company/jobs");
        mvc.perform(post("/company/jobs").session(company).with(csrf())
                        .param("title", "Data Intern").param("description", "ต้องการ Python, SQL และ Docker"))
                .andExpect(status().is3xxRedirection());

        // 4) นักศึกษาเห็นคะแนนบน Dashboard ทันที (66.7 ขาด Docker)
        MvcResult dash = mvc.perform(get("/student/dashboard").session(student))
                .andExpect(status().isOk())
                .andExpect(model().attribute("hasResume", true))
                .andReturn();
        @SuppressWarnings("unchecked")
        List<MatchView> list = (List<MatchView>) dash.getModelAndView().getModel().get("matches");
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getJobTitle()).isEqualTo("Data Intern");
        assertThat(list.get(0).getCompanyName()).isEqualTo("บริษัท เส้นทางหลัก");
        assertThat(list.get(0).getScore()).isEqualTo(66.7);
        assertThat(list.get(0).getMissing()).containsExactly("Docker");
        assertThat(dash.getResponse().getContentAsString()).contains("66.7%").contains("Docker");

        // 5) logout ได้ และหลัง logout (ไม่มี session) เข้าหน้าเดิมต้องถูกส่งไป login
        mvc.perform(post("/logout").session(student).with(csrf())).andExpect(redirectedUrl("/login?logout"));
        mvc.perform(get("/student/dashboard")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
