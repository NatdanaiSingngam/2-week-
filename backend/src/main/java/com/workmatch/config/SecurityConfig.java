package com.workmatch.config;

import com.workmatch.security.RoleRedirectSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * งาน A2: form login + BCrypt + แยกสิทธิ์ STUDENT / COMPANY
 * CSRF เปิดตามค่าเริ่มต้น (ฟอร์ม Thymeleaf ที่ใช้ th:action ได้ token อัตโนมัติ)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // TODO(integration, ฝั่งเรา - A5): /error ยังไม่มี template ของเรา จึงขึ้นหน้า Whitelabel ภาษาอังกฤษ
                //   ตอนโดนห้าม (403) หรือไม่พบหน้า (404) ให้เพิ่ม templates/error/403.html, 404.html และ error.html
                .requestMatchers("/login", "/register", "/css/**", "/js/**", "/images/**", "/error").permitAll()
                .requestMatchers("/student/**").hasRole("STUDENT")
                .requestMatchers("/company/**").hasRole("COMPANY")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")      // ช่องอีเมลในฟอร์มชื่อ username
                .passwordParameter("password")
                .successHandler(new RoleRedirectSuccessHandler())
                .failureUrl("/login?error")
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")               // ต้องเป็น POST พร้อม CSRF token
                .logoutSuccessUrl("/login?logout")
                .permitAll());
        return http.build();
    }
}
