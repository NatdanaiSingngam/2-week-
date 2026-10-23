package com.workmatch;

import com.workmatch.entity.Role;
import com.workmatch.entity.User;
import com.workmatch.nlp.NlpUnavailableException;
import com.workmatch.repository.UserRepository;
import com.workmatch.service.MatchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** งาน A5: หน้า error ภาษาไทย ไม่มี stack trace */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ErrorHandlingTests {

    @Autowired MockMvc mvc;
    @Autowired TestRestTemplate rest;
    @Autowired UserRepository users;
    @MockBean MatchService matchService;

    private void ensureStudent() {
        if (users.existsByEmail("err@test.com")) return;
        User u = new User();
        u.setEmail("err@test.com");
        u.setDisplayName("err");
        u.setPasswordHash("$2a$10$xxxxxxxxxxxxxxxxxxxxxuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuu");
        u.setRole(Role.STUDENT);
        users.save(u);
    }

    @Test
    void unexpectedErrorShowsThaiErrorPage() throws Exception {
        ensureStudent();
        when(matchService.dashboardFor(any())).thenThrow(new IllegalStateException("boom-internal-detail"));
        String html = mvc.perform(get("/student/dashboard").with(user("err@test.com").roles("STUDENT")))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error"))
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("เกิดข้อผิดพลาดในระบบ").doesNotContain("boom-internal-detail").doesNotContain("IllegalStateException");
    }

    @Test
    void nlpDownShowsThaiMessage() throws Exception {
        ensureStudent();
        when(matchService.dashboardFor(any())).thenThrow(new NlpUnavailableException());
        mvc.perform(get("/student/dashboard").with(user("err@test.com").roles("STUDENT")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("errorMessage", "ระบบวิเคราะห์ทักษะยังไม่พร้อม ลองใหม่อีกครั้ง"));
    }

    private ResponseEntity<String> html(HttpMethod method, String path) {
        HttpHeaders h = new HttpHeaders();
        h.setAccept(List.of(MediaType.TEXT_HTML));
        return rest.exchange(path, method, new HttpEntity<>(h), String.class);
    }

    @Test
    void notFoundUsesThai404Page() {
        ResponseEntity<String> r = html(HttpMethod.GET, "/css/no-such-file.css");
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(r.getBody()).contains("ไม่พบหน้าที่ต้องการ").doesNotContain("Whitelabel");
    }

    @Test
    void forbiddenUsesThai403Page() {
        // POST โดยไม่มี CSRF token -> 403
        ResponseEntity<String> r = html(HttpMethod.POST, "/register");
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(r.getBody()).contains("คุณไม่มีสิทธิ์เข้าถึงหน้านี้").doesNotContain("Whitelabel");
    }

    @Test
    void jsonErrorHasNoStackTrace() {
        HttpHeaders h = new HttpHeaders();
        h.setAccept(List.of(MediaType.APPLICATION_JSON));
        ResponseEntity<String> r = rest.exchange("/css/no-such-file.css", HttpMethod.GET, new HttpEntity<>(h), String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(r.getBody()).doesNotContain("trace").doesNotContain("exception");
    }
}
