package com.workmatch;

import com.workmatch.entity.Role;
import com.workmatch.entity.User;
import com.workmatch.nlp.ExtractedSkill;
import com.workmatch.nlp.NlpClient;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** TODO ข้อ 3 (แบบที่ 2): ไม่พบตำแหน่งงาน / ของบริษัทอื่น -> 404 + หน้า job-detail ส่วน "ไม่พบตำแหน่งงาน" ไม่รั่วข้อมูล */
@SpringBootTest
@AutoConfigureMockMvc
class JobNotFoundPageTest {

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
    void setUp() {
        matches.deleteAll();
        resumeSkills.deleteAll();
        resumes.deleteAll();
        jobSkills.deleteAll();
        jobs.deleteAll();
        users.deleteAll();
        for (String email : List.of("a@co.com", "b@co.com")) {
            User u = new User();
            u.setEmail(email);
            u.setDisplayName(email);
            u.setPasswordHash(encoder.encode("password123"));
            u.setRole(Role.COMPANY);
            users.save(u);
        }
        when(nlpClient.extractSkills(anyString())).thenReturn(List.of(new ExtractedSkill("Java", "programming")));
    }

    @Test
    void otherCompanysJobShowsNotFoundSectionWithoutLeakingData() throws Exception {
        String location = mvc.perform(post("/company/jobs").with(csrf()).with(user("a@co.com").roles("COMPANY"))
                        .param("title", "Secret Job Of A").param("description", "รายละเอียดลับของบริษัท A ใช้ Java"))
                .andReturn().getResponse().getRedirectedUrl();

        String html = mvc.perform(get(location).with(user("b@co.com").roles("COMPANY")))
                .andExpect(status().isNotFound())
                .andExpect(view().name("company/job-detail"))
                .andExpect(model().attributeDoesNotExist("job"))
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("ไม่พบตำแหน่งงาน").contains("กลับไปรายการตำแหน่ง")
                .doesNotContain("Secret Job Of A").doesNotContain("รายละเอียดลับ");

        // เจ้าของยังเปิดได้ตามปกติ
        mvc.perform(get(location).with(user("a@co.com").roles("COMPANY")))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("job"));
    }

    @Test
    void missingJobShowsNotFoundSection() throws Exception {
        String html = mvc.perform(get("/company/jobs/999999").with(user("a@co.com").roles("COMPANY")))
                .andExpect(status().isNotFound())
                .andExpect(view().name("company/job-detail"))
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("ไม่พบตำแหน่งงาน");
    }
}
