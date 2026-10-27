package com.workmatch;

import com.workmatch.entity.Role;
import com.workmatch.entity.User;
import com.workmatch.nlp.ExtractedSkill;
import com.workmatch.nlp.NlpClient;
import com.workmatch.repository.*;
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

/** TODO ข้อ 2 (27 ต.ค.): หัวข้อหมวดทักษะในหน้า resume ต้องเป็นชื่อไทย ไม่ใช่คีย์อังกฤษจาก NLP */
@SpringBootTest
@AutoConfigureMockMvc
class ResumeCategoryLabelTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @MockBean NlpClient nlpClient;

    @Test
    void categoryHeadingsAreThai() throws Exception {
        if (!users.existsByEmail("cat@test.com")) {
            User u = new User();
            u.setEmail("cat@test.com");
            u.setDisplayName("cat");
            u.setPasswordHash(encoder.encode("password123"));
            u.setRole(Role.STUDENT);
            users.save(u);
        }
        when(nlpClient.extractSkills(anyString())).thenReturn(List.of(
                new ExtractedSkill("Java", "programming"),
                new ExtractedSkill("การทำงานเป็นทีม", "soft"),
                new ExtractedSkill("Something", "brand-new-category")));
        mvc.perform(post("/student/resume").with(csrf()).with(user("cat@test.com").roles("STUDENT")).param("text", "Java"));

        String html = mvc.perform(get("/student/resume").with(user("cat@test.com").roles("STUDENT")))
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("<h3>ภาษาโปรแกรม</h3>")
                .contains("<h3>ทักษะทางสังคม (Soft skills)</h3>")
                .contains("<h3>brand-new-category</h3>")   // หมวดใหม่ที่ยังไม่มีชื่อไทย แสดงคีย์เดิม
                .doesNotContain("<h3>programming</h3>")
                .doesNotContain("<h3>soft</h3>");
    }
}
