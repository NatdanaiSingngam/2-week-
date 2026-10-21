package com.workmatch.service;

import com.workmatch.entity.Resume;
import com.workmatch.entity.ResumeSkill;
import com.workmatch.entity.Role;
import com.workmatch.entity.User;
import com.workmatch.nlp.ExtractedSkill;
import com.workmatch.nlp.NlpClient;
import com.workmatch.nlp.NlpUnavailableException;
import com.workmatch.repository.ResumeRepository;
import com.workmatch.repository.ResumeSkillRepository;
import org.junit.jupiter.api.BeforeEach;
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
class ResumeServiceTest {

    @Mock ResumeRepository resumes;
    @Mock ResumeSkillRepository resumeSkills;
    @Mock NlpClient nlpClient;
    @InjectMocks ResumeService service;

    User student;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(1L);
        student.setRole(Role.STUDENT);
    }

    @Test
    @SuppressWarnings("unchecked")
    void saveResumeStoresTextAndExtractedSkills() {
        when(nlpClient.extractSkills("มีความรู้ Python, Java และ SQL")).thenReturn(List.of(
                new ExtractedSkill("Python", "programming"),
                new ExtractedSkill("Java", "programming"),
                new ExtractedSkill("SQL", "database"),
                new ExtractedSkill("java", "programming"))); // ซ้ำ ต้องถูกตัด
        when(resumes.findByUserId(1L)).thenReturn(Optional.empty());
        when(resumes.save(any(Resume.class))).thenAnswer(inv -> {
            Resume r = inv.getArgument(0);
            r.setId(10L);
            return r;
        });

        Resume saved = service.saveResume(student, "  มีความรู้ Python, Java และ SQL  ");

        assertThat(saved.getRawText()).isEqualTo("มีความรู้ Python, Java และ SQL");
        assertThat(saved.getUser()).isSameAs(student);
        verify(resumeSkills).deleteByResumeId(10L);
        ArgumentCaptor<List<ResumeSkill>> rows = ArgumentCaptor.forClass(List.class);
        verify(resumeSkills).saveAll(rows.capture());
        assertThat(rows.getValue()).extracting(ResumeSkill::getSkillName).containsExactly("Python", "Java", "SQL");
    }

    @Test
    void saveResumeUpdatesExistingResumeInsteadOfCreatingNew() {
        Resume existing = new Resume();
        existing.setId(5L);
        existing.setUser(student);
        existing.setRawText("เก่า");
        when(nlpClient.extractSkills(anyString())).thenReturn(List.of(new ExtractedSkill("Git", "tool")));
        when(resumes.findByUserId(1L)).thenReturn(Optional.of(existing));
        when(resumes.save(existing)).thenReturn(existing);

        Resume saved = service.saveResume(student, "ใช้ Git");

        assertThat(saved.getId()).isEqualTo(5L);
        assertThat(saved.getRawText()).isEqualTo("ใช้ Git");
        verify(resumeSkills).deleteByResumeId(5L);
    }

    @Test
    void nlpFailureKeepsOldDataUntouched() {
        when(nlpClient.extractSkills(anyString())).thenThrow(new NlpUnavailableException());

        assertThatThrownBy(() -> service.saveResume(student, "Java"))
                .isInstanceOf(NlpUnavailableException.class)
                .hasMessage("ระบบวิเคราะห์ทักษะยังไม่พร้อม ลองใหม่อีกครั้ง");
        verify(resumes, never()).save(any());
        verify(resumeSkills, never()).deleteByResumeId(anyLong());
        verify(resumeSkills, never()).saveAll(anyList());
    }

    @Test
    void rejectsEmptyAndTooLongText() {
        assertThatThrownBy(() -> service.saveResume(student, "   ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.saveResume(student, "a".repeat(20001))).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(nlpClient);
    }
}
