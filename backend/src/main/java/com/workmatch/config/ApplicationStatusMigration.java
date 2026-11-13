/*
 * ==== Workmatch · Backend · ตั้งค่าระบบ ====
 * ไฟล์: backend/src/main/java/com/workmatch/config/ApplicationStatusMigration.java
 * หน้าที่: แปลงสถานะใบสมัครชื่อเดิม (เวอร์ชัน 1) ในฐานข้อมูลให้เป็นชื่อตามเอกสาร (เวอร์ชัน 2) ตอนเปิดระบบ
 *     INTERVIEW -> REVIEWED, HIRED -> ACCEPTED (PENDING, REJECTED ชื่อเดิม)
 * อ่าน/เขียนตาราง: job_applications (คอลัมน์ status) ผ่าน JdbcTemplate
 * รันก่อน: DemoDataLoader และ AdminAccountInitializer (@Order(0)) ทำเฉพาะ MySQL/MariaDB รันซ้ำได้ ไม่มีข้อมูลเดิมก็ไม่ทำอะไร
 */
package com.workmatch.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(0)
public class ApplicationStatusMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ApplicationStatusMigration.class);

    private final JdbcTemplate jdbc;

    public ApplicationStatusMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        // ข้อมูลชื่อเดิมมีได้เฉพาะฐานข้อมูล MySQL ที่ใช้งานมาตั้งแต่เวอร์ชัน 1 (H2 ตอนทดสอบสร้างใหม่ทุกครั้ง)
        if (!isMySql()) return;
        try {
            // Hibernate สร้างคอลัมน์ status เป็น ENUM('PENDING','INTERVIEW','HIRED','REJECTED') ไว้ตั้งแต่เวอร์ชัน 1
            // และ ddl-auto=update จะไม่แก้ชนิดคอลัมน์เดิม จึงต้องเปลี่ยนเป็น VARCHAR ก่อน ไม่อย่างนั้นบันทึกค่าใหม่ไม่ได้
            List<String> types = jdbc.queryForList(
                    "SELECT DATA_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() "
                            + "AND TABLE_NAME = 'job_applications' AND COLUMN_NAME = 'status'", String.class);
            if (types.isEmpty()) return; // ยังไม่มีตาราง
            if ("enum".equalsIgnoreCase(types.get(0))) {
                jdbc.execute("ALTER TABLE job_applications MODIFY status VARCHAR(10) NOT NULL");
                log.info("[migration] เปลี่ยนคอลัมน์ job_applications.status จาก ENUM เป็น VARCHAR(10)");
            }
            int reviewed = jdbc.update("UPDATE job_applications SET status = 'REVIEWED' WHERE status = 'INTERVIEW'");
            int accepted = jdbc.update("UPDATE job_applications SET status = 'ACCEPTED' WHERE status = 'HIRED'");
            if (reviewed + accepted > 0) {
                log.info("[migration] แปลงสถานะใบสมัคร INTERVIEW->REVIEWED {} แถว, HIRED->ACCEPTED {} แถว", reviewed, accepted);
            }
        } catch (RuntimeException e) {
            log.warn("[migration] แปลงสถานะใบสมัครเดิมไม่สำเร็จ: {}", e.getMessage());
        }
    }

    private boolean isMySql() {
        try {
            String product = jdbc.execute((ConnectionCallback<String>) c -> c.getMetaData().getDatabaseProductName());
            return product != null && (product.toLowerCase().contains("mysql") || product.toLowerCase().contains("mariadb"));
        } catch (RuntimeException e) {
            return false;
        }
    }
}
