package com.workmatch;

import com.workmatch.entity.*;
import com.workmatch.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/** ทดสอบว่า entity และ repository ของงาน A1 บันทึก/ค้นหาได้ถูกต้อง */
@DataJpaTest
class RepositoryTests {

    @Autowired UserRepository users;
    @Autowired ResumeRepository resumes;
    @Autowired ResumeSkillRepository resumeSkills;
    @Autowired JobRepository jobs;
    @Autowired JobSkillRepository jobSkills;
    @Autowired MatchRepository matches;

    private User newUser(String email, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash("$2a$10$placeholderplaceholderplaceholderplaceholderpla");
        u.setDisplayName(email);
        u.setRole(role);
        return users.save(u);
    }

    @Test
    void findUserByEmail() {
        newUser("s1@test.com", Role.STUDENT);
        assertThat(users.findByEmail("s1@test.com")).isPresent();
        assertThat(users.existsByEmail("s1@test.com")).isTrue();
        assertThat(users.existsByEmail("none@test.com")).isFalse();
        assertThat(users.findByEmail("s1@test.com").get().getCreatedAt()).isNotNull();
    }

    @Test
    void resumeSkillsSaveAndDelete() {
        User s = newUser("s2@test.com", Role.STUDENT);
        Resume r = new Resume();
        r.setUser(s);
        r.setRawText("มีทักษะ Java และ SQL");
        r = resumes.save(r);
        resumeSkills.save(new ResumeSkill(r, "Java", "programming"));
        resumeSkills.save(new ResumeSkill(r, "SQL", "database"));

        assertThat(resumes.findByUserId(s.getId())).isPresent();
        assertThat(resumeSkills.findByResumeId(r.getId())).hasSize(2);
        resumeSkills.deleteByResumeId(r.getId());
        assertThat(resumeSkills.findByResumeId(r.getId())).isEmpty();
    }

    @Test
    void jobsAndMatchUniquePair() {
        User s = newUser("s3@test.com", Role.STUDENT);
        User c = newUser("c1@test.com", Role.COMPANY);
        Resume r = new Resume();
        r.setUser(s);
        r.setRawText("Python");
        r = resumes.save(r);
        Job j = new Job();
        j.setCompany(c);
        j.setTitle("Backend Intern");
        j.setDescription("ต้องการ Java และ Git");
        j = jobs.save(j);
        jobSkills.save(new JobSkill(j, "Java", "programming"));

        assertThat(jobs.findByCompanyId(c.getId())).hasSize(1);
        assertThat(jobSkills.findByJobId(j.getId())).hasSize(1);

        matches.saveAndFlush(newMatch(r, j));
        assertThat(matches.findByResumeIdAndJobId(r.getId(), j.getId())).isPresent();
        assertThat(matches.findByResumeId(r.getId())).hasSize(1);
        assertThat(matches.findByJobId(j.getId())).hasSize(1);

        // คู่ resume_id + job_id ซ้ำต้องบันทึกไม่ได้
        Resume rr = r; Job jj = j;
        assertThatThrownBy(() -> matches.saveAndFlush(newMatch(rr, jj)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Match newMatch(Resume r, Job j) {
        Match m = new Match();
        m.setResume(r);
        m.setJob(j);
        m.setScore(new BigDecimal("50.0"));
        m.setCosine(new BigDecimal("70.7"));
        m.setMatchedSkills("[\"Java\"]");
        m.setMissingSkills("[\"Git\"]");
        return m;
    }
}
