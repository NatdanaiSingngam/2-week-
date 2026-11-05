package com.workmatch.service;

import com.workmatch.dto.JobDetail;
import com.workmatch.entity.Job;
import com.workmatch.entity.JobSkill;
import com.workmatch.entity.Role;
import com.workmatch.entity.User;
import com.workmatch.exception.JobNotFoundException;
import com.workmatch.nlp.ExtractedSkill;
import com.workmatch.nlp.NlpClient;
import com.workmatch.nlp.NlpUnavailableException;
import com.workmatch.repository.JobRepository;
import com.workmatch.repository.JobSkillRepository;
import com.workmatch.repository.MatchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock JobRepository jobs;
    @Mock JobSkillRepository jobSkills;
    @Mock NlpClient nlpClient;
    @Mock MatchRepository matches;
    @InjectMocks JobService service;

    private User company(long id) {
        User u = new User();
        u.setId(id);
        u.setRole(Role.COMPANY);
        return u;
    }

    @Test
    @SuppressWarnings("unchecked")
    void createJobSavesJobAndSkills() {
        User c = company(2L);
        when(nlpClient.extractSkills("ต้องการ Java และ Git")).thenReturn(List.of(
                new ExtractedSkill("Java", "programming"), new ExtractedSkill("Git", "tool")));
        when(jobs.save(any(Job.class))).thenAnswer(inv -> {
            Job j = inv.getArgument(0);
            j.setId(7L);
            return j;
        });

        Long id = service.createJob(c, "Backend Intern", "ต้องการ Java และ Git");

        assertThat(id).isEqualTo(7L);
        ArgumentCaptor<List<JobSkill>> rows = ArgumentCaptor.forClass(List.class);
        verify(jobSkills).saveAll(rows.capture());
        assertThat(rows.getValue()).extracting(JobSkill::getSkillName).containsExactly("Java", "Git");
    }

    @Test
    void createJobDoesNotSaveWhenNlpFails() {
        when(nlpClient.extractSkills(anyString())).thenThrow(new NlpUnavailableException());
        assertThatThrownBy(() -> service.createJob(company(2L), "Intern", "Java"))
                .isInstanceOf(NlpUnavailableException.class);
        verify(jobs, never()).save(any());
    }

    @Test
    void createJobValidatesTitle() {
        assertThatThrownBy(() -> service.createJob(company(2L), " ", "Java")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createJob(company(2L), "x".repeat(201), "Java")).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(nlpClient);
    }

    @Test
    void companyCannotOpenAnotherCompanysJob() {
        Job job = new Job();
        job.setId(9L);
        job.setCompany(company(3L)); // เป็นของบริษัท 3
        when(jobs.findById(9L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> service.getJobDetail(company(2L), 9L)).isInstanceOf(JobNotFoundException.class);
    }

    @Test
    void ownerSeesJobDetailWithSkills() {
        User owner = company(2L);
        Job job = new Job();
        job.setId(9L);
        job.setCompany(owner);
        job.setTitle("Data Intern");
        job.setDescription("Python");
        when(jobs.findById(9L)).thenReturn(Optional.of(job));
        when(jobSkills.findByJobId(9L)).thenReturn(List.of(new JobSkill(job, "Python", "programming")));

        JobDetail detail = service.getJobDetail(owner, 9L);

        assertThat(detail.getTitle()).isEqualTo("Data Intern");
        assertThat(detail.getSkills()).extracting("name").containsExactly("Python");
    }

    private Job ownedJob(User owner) {
        Job job = new Job();
        job.setId(9L);
        job.setCompany(owner);
        job.setTitle("เดิม");
        job.setDescription("Python");
        return job;
    }

    @Test
    @SuppressWarnings("unchecked")
    void updateJobReplacesTitleDescriptionAndSkills() {
        User owner = company(2L);
        Job job = ownedJob(owner);
        when(jobs.findById(9L)).thenReturn(Optional.of(job));
        when(nlpClient.extractSkills("ต้องการ Java")).thenReturn(List.of(new ExtractedSkill("Java", "programming")));

        service.updateJob(owner, 9L, " ใหม่ ", "ต้องการ Java");

        assertThat(job.getTitle()).isEqualTo("ใหม่");
        assertThat(job.getDescription()).isEqualTo("ต้องการ Java");
        verify(jobSkills).deleteByJobId(9L);
        ArgumentCaptor<List<JobSkill>> rows = ArgumentCaptor.forClass(List.class);
        verify(jobSkills).saveAll(rows.capture());
        assertThat(rows.getValue()).extracting(JobSkill::getSkillName).containsExactly("Java");
    }

    @Test
    void updateJobDoesNotChangeAnythingWhenNlpFails() {
        User owner = company(2L);
        when(jobs.findById(9L)).thenReturn(Optional.of(ownedJob(owner)));
        when(nlpClient.extractSkills(anyString())).thenThrow(new NlpUnavailableException());

        assertThatThrownBy(() -> service.updateJob(owner, 9L, "ใหม่", "Java")).isInstanceOf(NlpUnavailableException.class);
        verify(jobs, never()).save(any());
        verify(jobSkills, never()).deleteByJobId(any());
    }

    @Test
    void companyCannotUpdateOrDeleteAnotherCompanysJob() {
        when(jobs.findById(9L)).thenReturn(Optional.of(ownedJob(company(3L))));

        assertThatThrownBy(() -> service.updateJob(company(2L), 9L, "x", "y")).isInstanceOf(JobNotFoundException.class);
        assertThatThrownBy(() -> service.deleteJob(company(2L), 9L)).isInstanceOf(JobNotFoundException.class);
        verify(jobs, never()).delete(any());
        verify(matches, never()).deleteByJobId(any());
    }

    @Test
    void deleteJobRemovesMatchesSkillsAndJob() {
        User owner = company(2L);
        Job job = ownedJob(owner);
        when(jobs.findById(9L)).thenReturn(Optional.of(job));

        service.deleteJob(owner, 9L);

        verify(matches).deleteByJobId(9L);
        verify(jobSkills).deleteByJobId(9L);
        verify(jobs).delete(job);
    }
}
