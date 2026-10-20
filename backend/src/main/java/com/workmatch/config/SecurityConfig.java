package com.workmatch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * ชั่วคราว (งาน A1): เปิดทุก URL และปิด CSRF ไว้ก่อน เพื่อให้ทดสอบได้
 * งาน A2 จะแทนที่ไฟล์นี้ด้วย form login + BCrypt + แยกสิทธิ์ STUDENT/COMPANY
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll()) // ชั่วคราว
            .csrf(csrf -> csrf.disable());                                  // ชั่วคราว
        return http.build();
    }
}
