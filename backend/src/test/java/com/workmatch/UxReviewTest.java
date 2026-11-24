/*
 * ==== Workmatch · Backend · เทสต์ ====
 * หน้าที่: จุดที่แก้จากการตรวจ UX/UI: ตัวกรองไม่หายหลังกดดาว, ค้นหาสถานที่/รูปแบบงาน, แท็บ "ทั้งหมด", ย้อนสถานะ, แบ่งหน้าทักษะ
 */
package com.workmatch;

import com.workmatch.dto.Views.ExploreItem;
import com.workmatch.dto.Views.JobInput;
import com.workmatch.entity.Resume;
import com.workmatch.entity.User;
import com.workmatch.nlp.NlpClient;
import com.workmatch.repository.ResumeRepository;
import com.workmatch.service.ApplicationService;
import com.workmatch.service.DictionaryService;
import com.workmatch.service.JobPostingService;
import com.workmatch.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
class UxReviewTest {

    @Autowired MockMvc mvc;
    @Autowired RegistrationService registration;
    @Autowired JobPostingService postings;
    @Autowired ApplicationService applicationService;
    @Autowired ResumeRepository resumes;
    @Autowired DictionaryService dictionary;
    @MockBean NlpClient nlp;

    private User student() { return registration.register("นิสิต ยูเอ็กซ์", "ux-s@test.com", "password123", "STUDENT"); }
    private User company() { return registration.register("บริษัท ยูเอ็กซ์", "ux-c@test.com", "password123", "COMPANY"); }

    private Long job(User c, String title, String place, String mode, String skill) {
        when(nlp.extractSkills(anyString())).thenReturn(List.of());
        return postings.create(c, new JobInput(title, "รายละเอียด", "", "", "", "", List.of(skill), false, place, mode, "2", ""));
    }

    @Test
    void bookmarkReturnsToTheSameFilters() throws Exception {
        student();
        Long id = job(company(), "UX Alpha", "พะเยา", "Hybrid", "UxLang");
        String back = (String) mvc.perform(get("/student/jobs").param("q", "alpha").param("type", "ไอที / เทคโนโลยี")
                .with(user("ux-s@test.com").roles("STUDENT"))).andReturn().getModelAndView().getModel().get("back");
        assertThat(back).startsWith("/student/jobs?q=alpha&type=%E0%B9%84").contains("%2F").doesNotContain("+");
        mvc.perform(post("/student/jobs/" + id + "/bookmark").param("back", back).with(csrf())
                .with(user("ux-s@test.com").roles("STUDENT"))).andExpect(redirectedUrl(back));
        mvc.perform(post("/student/jobs/" + id + "/bookmark").param("back", "//evil.example/x").with(csrf())
                .with(user("ux-s@test.com").roles("STUDENT"))).andExpect(redirectedUrl("/student/jobs/" + id));
    }

    @Test
    void searchCoversPlaceAndModeAndPutsExactSkillFirst() {
        User s = student();
        User c = company();
        dictionary.addSkill("UxLang", "programming");
        dictionary.addSkill("UxLangScript", "programming");
        job(c, "UX Alpha", "ยูเอ็กซ์ซิตี้", "Hybrid", "UxLang");
        job(c, "UX Beta", "ที่อื่น", "Onsite", "UxLangScript");
        assertThat(postings.explore(s, "ยูเอ็กซ์ซิตี้", "").stream().map(ExploreItem::title)).containsExactly("UX Alpha");
        assertThat(postings.explore(s, "hybrid", "").stream().map(ExploreItem::title)).contains("UX Alpha").doesNotContain("UX Beta");
        List<String> java = postings.explore(s, "uxlang", "").stream().map(ExploreItem::title).filter(t -> t.startsWith("UX ")).toList();
        assertThat(java).containsExactly("UX Alpha", "UX Beta");
    }

    @Test
    void allTabIsDefaultAndStatusCanGoBackToPending() throws Exception {
        User s = student();
        Long id = job(company(), "UX Alpha", "พะเยา", "Hybrid", "UxLang");
        Resume r = new Resume();
        r.setUser(s);
        r.setRawText("Java");
        resumes.save(r);
        Long app = applicationService.apply(s, id, "");
        var co = user("ux-c@test.com").roles("COMPANY");
        mvc.perform(post("/company/applications/" + app + "/status").param("status", "ACCEPTED").with(csrf()).with(co));
        String html = mvc.perform(get("/company/jobs/" + id).with(co)).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("ทั้งหมด (1)").contains("นิสิต ยูเอ็กซ์");
        assertThat(mvc.perform(get("/company/applications/" + app).with(co)).andReturn().getResponse().getContentAsString())
                .contains("ย้อนเป็นรอพิจารณา");
        mvc.perform(post("/company/applications/" + app + "/status").param("status", "PENDING").with(csrf()).with(co));
        assertThat(mvc.perform(get("/company/applications/" + app).with(co)).andReturn().getResponse().getContentAsString())
                .contains("สถานะ: รอพิจารณา").doesNotContain("ย้อนเป็นรอพิจารณา");
    }

    @Test
    void skillDictionaryIsPaged() throws Exception {
        for (int i = 0; i < 30; i++) dictionary.addSkill("UxSkill" + i, "tools");
        var admin = user("ux-a@test.com").roles("ADMIN");
        var first = mvc.perform(get("/admin/skills").param("q", "UxSkill").with(admin)).andExpect(status().isOk()).andReturn().getModelAndView().getModel();
        assertThat((List<?>) first.get("skills")).hasSize(25);
        var last = mvc.perform(get("/admin/skills").param("q", "UxSkill").param("page", "99").with(admin)).andReturn().getModelAndView().getModel();
        assertThat((List<?>) last.get("skills")).hasSize(5);
    }
}
