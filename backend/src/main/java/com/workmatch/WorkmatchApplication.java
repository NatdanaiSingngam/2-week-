/*
 * ==== Workmatch · Backend · จุดเริ่มโปรแกรม ====
 * ไฟล์: backend/src/main/java/com/workmatch/WorkmatchApplication.java
 * หน้าที่: จุดเริ่มต้นของ Spring Boot รันไฟล์นี้ (หรือ mvnw spring-boot:run) เพื่อเปิดเว็บที่ http://localhost:8080
 */
package com.workmatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WorkmatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkmatchApplication.class, args);
    }
}
