package com.workmatch;

import com.workmatch.dto.ApplicantView;
import com.workmatch.dto.JobListItem;
import com.workmatch.dto.MatchView;
import com.workmatch.entity.Role;
import com.workmatch.entity.User;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** งาน A4: จับคู่ผ่าน /match (จำลอง) แล้วตรวจ Dashboard และหน้าผู้สมัคร */
@SpringBootTest
@AutoConfigureMockMvc
class MatchFlowTests {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired ResumeRepository resumes;
    @Autowired ResumeSkillRepository resumeSkills;
    @Autowired JobRepository jobs;
    @Autowired JobSkillRepository jobSkills;
    @Autowired MatchRepository matches;
    @Autowired PasswordEncoder encoder;
    @MockBean NlpClient nlpClient;

    // ข้อความ -> ทักษะ ที่ NLP จำลองจะตอบ
    private static final Map<String, List<String>> EXTRACT = Map.of(
            "resume-data", List.of("Python", "SQL"),
            "resume-backend", List.of("Java", "Git", "Python"),
            "resume-data-plus", List.of("Python", "SQL", "Docker"),
            "job-data", List.of("Python", "SQL", "Docker"),
            "job-backend", List.of("Java", "Git"),
            "job-python", List.of("Python"));

    @BeforeEach
    void setUp() {
        matches.deleteAll();
        resumeSkills.deleteAll();
        resumes.deleteAll();
        jobSkills.deleteAll();
        jobs.deleteAll();
        users.deleteAll();
        when(nlpClient.extractSkills(anyString())).thenAnswer(inv ->
                EXTRACT.getOrDefault(inv.<String>getArgument(0), List.of()).stream()
                        .map(n -> new ExtractedSkill(n, "x")).toList());
        // NLP จำลอง: score = ตรง/ที่งานต้องการ x 100 (เหมือน api-contract) ใช้เฉพาะในเทสต์
        when(nlpClient.match(anyList(), anyList())).thenAnswer(inv -> {
            List<String> have = inv.getArgument(0);
            List<String> need = inv.getArgument(1);
            List<String> matched = new ArrayList<>(), missing = new ArrayList<>();
            for (String s : need) (have.contains(s) ? matched : missing).add(s);
            double score = need.isEmpty() ? 0 : Math.round(matched.size() * 1000.0 / need.size()) / 10.0;
            return new MatchResult(score, score, matched, missing);
        });
    }

    private void createUser(String email, String name, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setDisplayName(name);
        u.setPasswordHash(encoder.encode("password123"));
        u.setRole(role);
        users.save(u);
    }

    private String postJob(String company, String title, String description) throws Exception {
        return mvc.perform(post("/company/jobs").with(csrf()).with(user(company).roles("COMPANY"))
                        .param("title", title).param("description", description))
                .andReturn().getResponse().getRedirectedUrl();
    }

    private void postResume(String student, String text) throws Exception {
        mvc.perform(post("/student/resume").with(csrf()).with(user(student).roles("STUDENT")).param("text", text));
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> modelList(String url, String userName, String role, String attr) throws Exception {
        return (List<T>) mvc.perform(get(url).with(user(userName).roles(role)))
                .andExpect(status().isOk())
                .andReturn().getModelAndView().getModel().get(attr);
    }

    @Test
    void fullMatchingFlow() throws Exception {
        createUser("s1@test.com", "นักศึกษา 1", Role.STUDENT);
        createUser("s2@test.com", "นักศึกษา 2", Role.STUDENT);
        createUser("a@co.com", "บริษัท A", Role.COMPANY);
        createUser("b@co.com", "บริษัท B", Role.COMPANY);

        String dataJob = postJob("a@co.com", "Data Intern", "job-data");
        String backendJob = postJob("a@co.com", "Backend Intern", "job-backend");
        postResume("s1@test.com", "resume-data");
        postResume("s2@test.com", "resume-backend");

        // คะแนนถูกเก็บ: Python, SQL กับงาน Python, SQL, Docker = 66.7 ขาด Docker
        List<MatchView> s1 = modelList("/student/dashboard", "s1@test.com", "STUDENT", "matches");
        assertThat(s1).extracting(MatchView::getJobTitle).containsExactly("Data Intern", "Backend Intern");
        assertThat(s1.get(0).getScore()).isEqualTo(66.7);
        assertThat(s1.get(0).getMissing()).containsExactly("Docker");
        assertThat(s1.get(0).getMatched()).containsExactly("Python", "SQL");
        assertThat(s1.get(0).getCompanyName()).isEqualTo("บริษัท A");
        assertThat(matches.count()).isEqualTo(4);

        // คำนวณซ้ำไม่เกิดแถวซ้ำ และคะแนนเปลี่ยนตาม resume ใหม่
        postResume("s1@test.com", "resume-data-plus");
        assertThat(matches.count()).isEqualTo(4);
        List<MatchView> s1Again = modelList("/student/dashboard", "s1@test.com", "STUDENT", "matches");
        assertThat(s1Again.get(0).getScore()).isEqualTo(100.0);
        assertThat(s1Again.get(0).getMissing()).isEmpty();

        // dashboard เรียงคะแนนมากไปน้อย
        List<MatchView> s2 = modelList("/student/dashboard", "s2@test.com", "STUDENT", "matches");
        assertThat(s2).extracting(MatchView::getScore).containsExactly(100.0, 33.3);

        // ประกาศใหม่ -> นักศึกษาที่มี resume เห็นทันที
        postJob("b@co.com", "Python Intern", "job-python");
        assertThat(modelList("/student/dashboard", "s1@test.com", "STUDENT", "matches")).hasSize(3);

        // หน้าผู้สมัครของบริษัท A เรียงคะแนน ไม่มีอีเมล
        List<ApplicantView> applicants = modelList(dataJob, "a@co.com", "COMPANY", "applicants");
        assertThat(applicants).extracting(ApplicantView::getStudentName).containsExactly("นักศึกษา 1", "นักศึกษา 2");
        assertThat(applicants).extracting(ApplicantView::getScore).containsExactly(100.0, 33.3);
        String html = mvc.perform(get(dataJob).with(user("a@co.com").roles("COMPANY")))
                .andReturn().getResponse().getContentAsString();
        assertThat(html).doesNotContain("s1@test.com").doesNotContain("s2@test.com");

        // applicantCount นับเฉพาะคะแนนมากกว่า 0 (s1 ได้ 0 กับงาน backend)
        List<JobListItem> jobsOfA = modelList("/company/jobs", "a@co.com", "COMPANY", "jobs");
        assertThat(jobsOfA).extracting(JobListItem::getTitle, JobListItem::getApplicantCount)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("Data Intern", 2L),
                        org.assertj.core.groups.Tuple.tuple("Backend Intern", 1L));

        // บริษัท B ไม่เห็นผู้สมัครของงานบริษัท A
        mvc.perform(get(backendJob).with(user("b@co.com").roles("COMPANY"))).andExpect(status().isNotFound());
    }

    @Test
    void studentWithoutResumeGetsEmptyMatches() throws Exception {
        createUser("s1@test.com", "นักศึกษา 1", Role.STUDENT);
        List<MatchView> list = modelList("/student/dashboard", "s1@test.com", "STUDENT", "matches");
        assertThat(list).isEmpty();
    }
}
