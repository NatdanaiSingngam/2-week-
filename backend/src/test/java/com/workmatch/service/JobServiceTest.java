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
}
