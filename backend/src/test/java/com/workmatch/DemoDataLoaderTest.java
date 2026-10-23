package com.workmatch;

import com.workmatch.demo.DemoDataLoader;
import com.workmatch.entity.User;
import com.workmatch.nlp.ExtractedSkill;
import com.workmatch.nlp.MatchResult;
import com.workmatch.nlp.NlpClient;
import com.workmatch.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** งาน A5: ข้อมูลตัวอย่าง (profile demo) สร้างครบ เข้ารหัส BCrypt มีคะแนน และรันซ้ำไม่สร้างซ้ำ */
@SpringBootTest
@ActiveProfiles("demo")
class DemoDataLoaderTest {

    @Autowired DemoDataLoader loader;
    @Autowired UserRepository users;
    @Autowired ResumeRepository resumes;
    @Autowired ResumeSkillRepository resumeSkills;
    @Autowired JobRepository jobs;
    @Autowired JobSkillRepository jobSkills;
    @Autowired MatchRepository matches;
    @Autowired PasswordEncoder encoder;
    @MockBean NlpClient nlpClient;

    @Test
    void createsDemoAccountsResumesJobsAndScoresOnce() {
        matches.deleteAll();
        resumeSkills.deleteAll();
        resumes.deleteAll();
        jobSkills.deleteAll();
        jobs.deleteAll();
        users.deleteAll();
        when(nlpClient.extractSkills(anyString())).thenReturn(List.of(
                new ExtractedSkill("Python", "programming"), new ExtractedSkill("Git", "tools")));
        when(nlpClient.match(anyList(), anyList())).thenReturn(new MatchResult(50.0, 50.0, List.of("Python"), List.of("Git")));

        loader.run();

        assertThat(users.count()).isEqualTo(5);
        assertThat(resumes.count()).isEqualTo(3);
        assertThat(jobs.count()).isEqualTo(6);
        assertThat(matches.count()).isEqualTo(18); // 3 เรซูเม่ x 6 ตำแหน่ง
        User s1 = users.findByEmail(DemoDataLoader.MARKER_EMAIL).orElseThrow();
        assertThat(s1.getPasswordHash()).startsWith("$2");
        assertThat(encoder.matches(DemoDataLoader.DEMO_PASSWORD, s1.getPasswordHash())).isTrue();

        loader.run(); // รันซ้ำต้องไม่สร้างซ้ำ
        assertThat(users.count()).isEqualTo(5);
        assertThat(jobs.count()).isEqualTo(6);
        assertThat(matches.count()).isEqualTo(18);
    }
}
