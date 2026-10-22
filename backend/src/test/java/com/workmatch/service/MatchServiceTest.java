package com.workmatch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workmatch.entity.*;
import com.workmatch.nlp.MatchResult;
import com.workmatch.nlp.NlpClient;
import com.workmatch.nlp.NlpUnavailableException;
import com.workmatch.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock MatchRepository matches;
    @Mock JobRepository jobs;
    @Mock JobSkillRepository jobSkills;
    @Mock ResumeRepository resumes;
    @Mock ResumeSkillRepository resumeSkills;
    @Mock NlpClient nlpClient;
    MatchService service;

    Resume resume;
    Job job;

    @BeforeEach
    void setUp() {
        service = new MatchService(matches, jobs, jobSkills, resumes, resumeSkills, nlpClient, new ObjectMapper());
        resume = new Resume();
        resume.setId(1L);
        job = new Job();
        job.setId(2L);
        when(resumes.findById(1L)).thenReturn(Optional.of(resume));
        when(resumeSkills.findByResumeId(1L)).thenReturn(List.of(
                new ResumeSkill(resume, "Python", "programming"), new ResumeSkill(resume, "SQL", "database")));
    }

    @Test
    void scoreFromNlpIsStoredWithJsonSkillLists() {
        when(jobs.findAll()).thenReturn(List.of(job));
        when(jobSkills.findByJobId(2L)).thenReturn(List.of(
                new JobSkill(job, "Python", "programming"), new JobSkill(job, "SQL", "database"), new JobSkill(job, "Docker", "tools")));
        when(nlpClient.match(List.of("Python", "SQL"), List.of("Python", "SQL", "Docker")))
                .thenReturn(new MatchResult(66.66666, 81.649, List.of("Python", "SQL"), List.of("Docker")));
        when(matches.findByResumeIdAndJobId(1L, 2L)).thenReturn(Optional.empty());

        assertThat(service.recalculateForResume(1L)).isEqualTo(1);

        ArgumentCaptor<Match> saved = ArgumentCaptor.forClass(Match.class);
        verify(matches).save(saved.capture());
        assertThat(saved.getValue().getScore()).isEqualByComparingTo(new BigDecimal("66.7"));
        assertThat(saved.getValue().getCosine()).isEqualByComparingTo(new BigDecimal("81.6"));
        assertThat(saved.getValue().getMatchedSkills()).isEqualTo("[\"Python\",\"SQL\"]");
        assertThat(saved.getValue().getMissingSkills()).isEqualTo("[\"Docker\"]");
    }

    @Test
    void recalculatingUpdatesExistingRowInsteadOfAddingOne() {
        Match existing = new Match();
        existing.setResume(resume);
        existing.setJob(job);
        existing.setScore(new BigDecimal("10.0"));
        when(jobs.findAll()).thenReturn(List.of(job));
        when(jobSkills.findByJobId(2L)).thenReturn(List.of(new JobSkill(job, "Python", "programming")));
        when(nlpClient.match(anyList(), anyList())).thenReturn(new MatchResult(100.0, 100.0, List.of("Python"), List.of()));
        when(matches.findByResumeIdAndJobId(1L, 2L)).thenReturn(Optional.of(existing));

        service.recalculateForResume(1L);

        verify(matches).save(same(existing));
        assertThat(existing.getScore()).isEqualByComparingTo(new BigDecimal("100.0"));
    }

    @Test
    void jobWithoutSkillsIsSkipped() {
        when(jobs.findAll()).thenReturn(List.of(job));
        when(jobSkills.findByJobId(2L)).thenReturn(List.of());

        assertThat(service.recalculateForResume(1L)).isZero();
        verifyNoInteractions(nlpClient);
        verify(matches, never()).save(any());
    }

    @Test
    void nlpFailurePropagatesSoCallerCanWarn() {
        when(jobs.findAll()).thenReturn(List.of(job));
        when(jobSkills.findByJobId(2L)).thenReturn(List.of(new JobSkill(job, "Python", "programming")));
        when(nlpClient.match(anyList(), anyList())).thenThrow(new NlpUnavailableException());

        assertThatThrownBy(() -> service.recalculateForResume(1L)).isInstanceOf(NlpUnavailableException.class);
        verify(matches, never()).save(any());
    }
}
