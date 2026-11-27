/*
 * ==== Workmatch · Backend · เทสต์ ====
 * หน้าที่: ข้อมูลที่เก็บเพิ่มทั้งสองฝั่ง (GPA/ช่วงสหกิจ, สถานที่/วันปิดรับ, เว็บไซต์บริษัท), ความยินยอม GPA, หน้า login หมดอายุ
 */
package com.workmatch;

import com.workmatch.dto.Views.JobInput;
import com.workmatch.entity.Job;
import com.workmatch.entity.Resume;
import com.workmatch.entity.User;
import com.workmatch.nlp.NlpClient;
import com.workmatch.repository.JobRepository;
import com.workmatch.repository.ResumeRepository;
import com.workmatch.service.ApplicationService;
import com.workmatch.service.JobPostingService;
import com.workmatch.service.ProfileService;
import com.workmatch.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProfileDataCollectionTest {

    @Autowired MockMvc mvc;
    @Autowired RegistrationService registration;
    @Autowired ProfileService profiles;
    @Autowired JobPostingService postings;
    @Autowired ApplicationService applicationService;
    @Autowired JobRepository jobs;
    @Autowired ResumeRepository resumes;
    @MockBean NlpClient nlp;

    private User student() { return registration.register("นิสิต ทดสอบ", "pdc-s@test.com", "password123", "STUDENT"); }
    private User company() { return registration.register("บริษัท พีดีซี", "pdc-c@test.com", "password123", "COMPANY"); }

    private Long job(User c, String deadline) {
        when(nlp.extractSkills(anyString())).thenReturn(List.of());
        return postings.create(c, new JobInput("Dev", "Java", "", "", "", "", List.of(), false, "พะเยา", "Hybrid", "2", deadline));
    }

    @Test
    void gpaIsHiddenFromCompanyUntilStudentConsents() {
        User s = student();
        User c = company();
        Long jobId = job(c, "");
        Resume r = new Resume();
        r.setUser(s);
        r.setRawText("Java");
        resumes.save(r);
        Long appId = applicationService.apply(s, jobId, null);

        profiles.updateStudent(s, "นิสิต ทดสอบ", "", "", "", "", "3.25", false, "2027-01-04", "2027-04-30");
        assertThat(applicationService.candidate(c, appId).gpa()).isNull();
        assertThat(applicationService.candidate(c, appId).coopStart()).isEqualTo(LocalDate.of(2027, 1, 4));

        profiles.updateStudent(s, "นิสิต ทดสอบ", "", "", "", "", "3.25", true, "2027-01-04", "2027-04-30");
        assertThat(applicationService.candidate(c, appId).gpa()).isEqualByComparingTo("3.25");
    }

    @Test
    void invalidValuesAreRejected() {
        User s = student();
        User c = company();
        for (String gpa : List.of("4.01", "-1", "abc", "3.255")) {
            assertThatThrownBy(() -> profiles.updateStudent(s, "ก ข", "", "", "", "", gpa, false, "", ""))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> profiles.updateStudent(s, "ก ข", "", "", "", "", "", false, "2027-05-01", "2027-04-01"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> profiles.updateCompany(c, "บ", "", "", "", "", "", "", "javascript:alert(1)", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> job(c, LocalDate.now().minusDays(1).toString())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void expiredJobStopsAcceptingButTodayIsStillOpen() {
        User s = student();
        Long id = job(company(), LocalDate.now().toString());
        assertThat(postings.explore(s, "", "")).anyMatch(e -> e.jobId().equals(id));
        Job j = jobs.findById(id).orElseThrow();
        j.setDeadline(LocalDate.now().minusDays(1));
        jobs.save(j);
        assertThat(postings.explore(s, "", "")).noneMatch(e -> e.jobId().equals(id));
        assertThat(postings.studentView(s, id).accepting()).isFalse();
        assertThatThrownBy(() -> applicationService.apply(s, id, null)).hasMessageContaining("ปิดรับสมัคร");
    }

    @Test
    void bothProfileFormsSaveAndRender() throws Exception {
        student();
        company();
        var asStudent = user("pdc-s@test.com").roles("STUDENT");
        var asCompany = user("pdc-c@test.com").roles("COMPANY");
        mvc.perform(post("/student/profile").with(csrf()).with(asStudent)
                .param("fullName", "นิสิต ทดสอบ").param("gpa", "3.5").param("gpaVisible", "true").param("coopStart", "2027-01-04"))
                .andExpect(redirectedUrl("/student/profile"));
        assertThat(mvc.perform(get("/student/profile").with(asStudent)).andReturn().getResponse().getContentAsString())
                .contains("value=\"3.50\"").doesNotContain("name=\"coopStart\"");
        mvc.perform(post("/company/profile").with(csrf()).with(asCompany)
                .param("companyName", "บริษัท พีดีซี").param("website", "https://example.com").param("about", "เกี่ยวกับเรา"))
                .andExpect(redirectedUrl("/company/profile"));
        assertThat(mvc.perform(get("/company/profile").with(asCompany)).andReturn().getResponse().getContentAsString())
                .contains("https://example.com").contains("เกี่ยวกับเรา");
        Long id = job(registration.register("บริษัท อีก", "pdc-c2@test.com", "password123", "COMPANY"), "2099-01-01");
        var owner = user("pdc-c2@test.com").roles("COMPANY");
        mvc.perform(get("/company/jobs/" + id + "/edit").with(owner)).andExpect(status().isOk());
        mvc.perform(get("/company/postings").with(owner)).andExpect(status().isOk());
        mvc.perform(get("/student/jobs/" + id).with(asStudent)).andExpect(status().isOk());
        mvc.perform(get("/student/jobs").with(asStudent)).andExpect(status().isOk());
    }

    @Test
    void extractSkillsButtonReturnsPreviewWithoutSaving() throws Exception {
        long before = jobs.count();
        when(nlp.extractSkills(anyString())).thenReturn(List.of(new com.workmatch.nlp.ExtractedSkill("Java", "programming")));
        var asCompany = user("pdc-c@test.com").roles("COMPANY");
        company();
        String json = mvc.perform(post("/company/jobs/extract-skills").with(csrf()).with(asCompany).param("description", "ใช้ Java"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json).isEqualTo("{\"skills\":[\"Java\"]}");
        assertThat(jobs.count()).isEqualTo(before);
        mvc.perform(post("/company/jobs/extract-skills").with(csrf()).with(asCompany).param("description", " ")).andExpect(status().isBadRequest());
        when(nlp.extractSkills(anyString())).thenThrow(new com.workmatch.nlp.NlpUnavailableException());
        mvc.perform(post("/company/jobs/extract-skills").with(csrf()).with(asCompany).param("description", "Java")).andExpect(status().isServiceUnavailable());
        mvc.perform(post("/company/jobs/extract-skills").with(csrf()).with(user("pdc-s@test.com").roles("STUDENT")).param("description", "Java")).andExpect(status().isForbidden());
    }

    @Test
    void staleLoginPageReturnsToLoginInsteadOf403() throws Exception {
        mvc.perform(post("/login").param("username", "x@test.com").param("password", "x")).andExpect(redirectedUrl("/login?expired"));
        mvc.perform(post("/register")).andExpect(status().isForbidden());
    }
}
