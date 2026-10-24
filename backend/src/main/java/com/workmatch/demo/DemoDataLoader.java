package com.workmatch.demo;

import com.workmatch.entity.Resume;
import com.workmatch.entity.User;
import com.workmatch.repository.UserRepository;
import com.workmatch.service.JobService;
import com.workmatch.service.MatchService;
import com.workmatch.service.RegistrationService;
import com.workmatch.service.ResumeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * งาน A5: ข้อมูลตัวอย่างสำหรับสาธิต ทำงานเฉพาะเมื่อเปิด profile "demo"
 * (mvnw spring-boot:run -Dspring-boot.run.profiles=demo หรือ start-demo.bat)
 *
 * - สร้างบัญชีผ่าน service เดียวกับผู้ใช้จริง: รหัสผ่านเข้ารหัส BCrypt, ทักษะมาจาก NLP /extract, คะแนนมาจาก /match
 * - รันซ้ำได้: ถ้ามีบัญชีตัวอย่างแล้วจะข้าม ไม่สร้างซ้ำ และไม่ลบข้อมูลอื่น
 * - รหัสผ่านทดลองเป็นค่าสำหรับสาธิตเท่านั้น (ไม่ใช่รหัสผ่านจริงของใคร)
 */
@Component
@Profile("demo")
public class DemoDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    public static final String DEMO_PASSWORD = "demo1234";
    public static final String MARKER_EMAIL = "student1@demo.workmatch.test";

    private record DemoUser(String email, String name, String role) { }
    private record DemoJob(int companyIndex, String title, String description) { }

    static final List<DemoUser> STUDENTS = List.of(
            new DemoUser("student1@demo.workmatch.test", "สมชาย ใจดี (ตัวอย่าง)", "STUDENT"),
            new DemoUser("student2@demo.workmatch.test", "สมหญิง รักเรียน (ตัวอย่าง)", "STUDENT"),
            new DemoUser("student3@demo.workmatch.test", "วิชัย ข้อมูลดี (ตัวอย่าง)", "STUDENT"));
    static final List<String> RESUME_FILES = List.of("demo/resume-backend.txt", "demo/resume-frontend.txt", "demo/resume-data.txt");

    static final List<DemoUser> COMPANIES = List.of(
            new DemoUser("company1@demo.workmatch.test", "บริษัท เทคโนโลยีตัวอย่าง จำกัด", "COMPANY"),
            new DemoUser("company2@demo.workmatch.test", "บริษัท ดิจิทัลสาธิต จำกัด", "COMPANY"));

    static final List<DemoJob> JOBS = List.of(
            new DemoJob(0, "Backend Developer (สหกิจศึกษา)",
                    "พัฒนา REST API ด้วย Java และ Spring Boot เชื่อมต่อฐานข้อมูล MySQL ใช้ Git และ Docker ในการทำงานร่วมกับทีม"),
            new DemoJob(0, "Frontend Developer (สหกิจศึกษา)",
                    "พัฒนาหน้าเว็บด้วย HTML CSS JavaScript และ React ร่วมกับทีมออกแบบที่ใช้ Figma ใช้ Git จัดการโค้ด"),
            new DemoJob(0, "Data Analyst (สหกิจศึกษา)",
                    "วิเคราะห์ข้อมูลด้วย Python และ SQL ทำรายงานด้วย Excel มีพื้นฐาน Machine Learning เป็นข้อได้เปรียบ"),
            new DemoJob(1, "Mobile App Developer (สหกิจศึกษา)",
                    "พัฒนาแอปมือถือด้วย Flutter และ Dart เชื่อมต่อ Firebase ใช้ Git ทำงานเป็นทีม"),
            new DemoJob(1, "DevOps / QA (สหกิจศึกษา)",
                    "ดูแลระบบด้วย Docker และ Linux เขียนสคริปต์ทดสอบด้วย Python ทดสอบ API ด้วย Postman ใช้ Git"),
            new DemoJob(1, "UX/UI Designer (สหกิจศึกษา)",
                    "ออกแบบหน้าจอด้วย Figma เข้าใจ HTML และ CSS มีทักษะการสื่อสารกับทีมพัฒนา"));

    private final UserRepository users;
    private final RegistrationService registrationService;
    private final ResumeService resumeService;
    private final JobService jobService;
    private final MatchService matchService;

    public DemoDataLoader(UserRepository users, RegistrationService registrationService, ResumeService resumeService,
                          JobService jobService, MatchService matchService) {
        this.users = users;
        this.registrationService = registrationService;
        this.resumeService = resumeService;
        this.jobService = jobService;
        this.matchService = matchService;
    }

    @Override
    public void run(String... args) {
        if (users.existsByEmail(MARKER_EMAIL)) {
            log.info("[demo] มีข้อมูลตัวอย่างอยู่แล้ว ข้ามการสร้าง");
            return;
        }
        try {
            load();
            log.info("[demo] สร้างข้อมูลตัวอย่างเสร็จ: นักศึกษา {} คน บริษัท {} แห่ง ประกาศงาน {} ตำแหน่ง",
                    STUDENTS.size(), COMPANIES.size(), JOBS.size());
        } catch (RuntimeException e) {
            // เช่น service NLP ยังไม่รัน: แอปยังเปิดได้ แค่ไม่มีข้อมูลตัวอย่าง
            log.warn("[demo] สร้างข้อมูลตัวอย่างไม่สำเร็จ (เปิด service NLP ก่อนแล้วรันใหม่): {}", e.getMessage());
        }
    }

    /** สร้างข้อมูลทั้งหมด (แยกออกมาเพื่อให้ทดสอบได้) */
    public void load() {
        List<User> companies = new ArrayList<>();
        for (DemoUser c : COMPANIES) {
            companies.add(registrationService.register(c.name(), c.email(), DEMO_PASSWORD, c.role()));
        }
        // ประกาศงาน: ใช้ JobService เดียวกับหน้า /company/jobs (ดึงทักษะผ่าน NLP)
        for (DemoJob j : JOBS) {
            jobService.createJob(companies.get(j.companyIndex()), j.title(), j.description());
        }
        // เรซูเม่: ใช้ ResumeService แล้วคำนวณคะแนนกับทุกประกาศ เหมือนตอนนักศึกษากดบันทึก
        for (int i = 0; i < STUDENTS.size(); i++) {
            DemoUser s = STUDENTS.get(i);
            User student = registrationService.register(s.name(), s.email(), DEMO_PASSWORD, s.role());
            Resume resume = resumeService.saveResume(student, readText(RESUME_FILES.get(i)));
            matchService.recalculateForResume(resume.getId());
        }
    }

    private static String readText(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new IllegalStateException("อ่านไฟล์ตัวอย่างไม่ได้: " + path, e);
        }
    }
}
