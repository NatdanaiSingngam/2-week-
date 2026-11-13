/*
 * ==== Workmatch · Backend · ข้อมูลสาธิต ====
 * ไฟล์: backend/src/main/java/com/workmatch/demo/DemoDataLoader.java
 * หน้าที่: สร้างบัญชีและข้อมูลตัวอย่างสำหรับสาธิต ทำงานเฉพาะเมื่อเปิด profile demo (start-demo.bat)
 * เรียกใช้: UserRepository, RegistrationService, ResumeService, JobService, MatchService,
 *     StudentProfileRepository, CompanyProfileRepository, JobRepository, JobApplicationRepository,
 *     BookmarkRepository, StoredFileRepository
 */
package com.workmatch.demo;

import com.workmatch.entity.ApplicationStatus;
import com.workmatch.entity.Bookmark;
import com.workmatch.entity.CompanyProfile;
import com.workmatch.entity.Job;
import com.workmatch.entity.JobApplication;
import com.workmatch.entity.Resume;
import com.workmatch.entity.StoredFile;
import com.workmatch.entity.StudentProfile;
import com.workmatch.entity.User;
import com.workmatch.entity.VerificationStatus;
import com.workmatch.repository.BookmarkRepository;
import com.workmatch.repository.CompanyProfileRepository;
import com.workmatch.repository.JobApplicationRepository;
import com.workmatch.repository.JobRepository;
import com.workmatch.repository.StoredFileRepository;
import com.workmatch.repository.StudentProfileRepository;
import com.workmatch.repository.UserRepository;
import com.workmatch.service.JobService;
import com.workmatch.service.MatchService;
import com.workmatch.service.RegistrationService;
import com.workmatch.service.ResumeService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * งาน A5: ข้อมูลตัวอย่างสำหรับสาธิต ทำงานเฉพาะเมื่อเปิด profile "demo"
 * (mvnw spring-boot:run -Dspring-boot.run.profiles=demo หรือ start-demo.bat)
 *
 * - สร้างบัญชีผ่าน service เดียวกับผู้ใช้จริง: รหัสผ่านเข้ารหัส BCrypt, ทักษะมาจาก NLP /extract, คะแนนมาจาก /match
 * - รันซ้ำได้: ถ้ามีบัญชีตัวอย่างแล้วจะไม่สร้างซ้ำ และไม่ลบข้อมูลอื่น
 * - ส่วนเสริมตามเอกสารบทที่ 3 (ผู้ดูแลระบบ โปรไฟล์ ใบสมัคร งานที่บันทึก คำขอยืนยันตัวตน) เติมให้ฐานข้อมูลเดิมได้ (loadExtras)
 * - รหัสผ่านทดลองเป็นค่าสำหรับสาธิตเท่านั้น (ไม่ใช่รหัสผ่านจริงของใคร) ข้อมูลบริษัท/นักศึกษาเป็นข้อมูลสมมติ
 */
@Component
@Profile("demo")
public class DemoDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    public static final String DEMO_PASSWORD = "demo1234";
    public static final String MARKER_EMAIL = "student1@demo.workmatch.test";
    public static final String ADMIN_EMAIL = "admin@demo.workmatch.test";

    private record DemoUser(String email, String name, String role) { }
    private record DemoJob(int companyIndex, String title, String description) { }

    static final List<DemoUser> STUDENTS = List.of(
            new DemoUser("student1@demo.workmatch.test", "สมชาย ใจดี (ตัวอย่าง)", "STUDENT"),
            new DemoUser("student2@demo.workmatch.test", "สมหญิง รักเรียน (ตัวอย่าง)", "STUDENT"),
            new DemoUser("student3@demo.workmatch.test", "วิชัย ข้อมูลดี (ตัวอย่าง)", "STUDENT"));
    static final List<String> RESUME_FILES = List.of("demo/resume-backend.txt", "demo/resume-frontend.txt", "demo/resume-data.txt");
    static final List<String[]> STUDENT_INFO = List.of(
            new String[]{"66000001", "คณะเทคโนโลยีสารสนเทศและการสื่อสาร", "วิทยาการคอมพิวเตอร์"},
            new String[]{"66000002", "คณะเทคโนโลยีสารสนเทศและการสื่อสาร", "วิศวกรรมซอฟต์แวร์"},
            new String[]{"66000003", "คณะเทคโนโลยีสารสนเทศและการสื่อสาร", "วิทยาการข้อมูลและการประยุกต์"});

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
    private final StudentProfileRepository studentProfiles;
    private final CompanyProfileRepository companyProfiles;
    private final JobRepository jobs;
    private final JobApplicationRepository applications;
    private final BookmarkRepository bookmarks;
    private final StoredFileRepository files;

    public DemoDataLoader(UserRepository users, RegistrationService registrationService, ResumeService resumeService,
                          JobService jobService, MatchService matchService, StudentProfileRepository studentProfiles,
                          CompanyProfileRepository companyProfiles, JobRepository jobs,
                          JobApplicationRepository applications, BookmarkRepository bookmarks, StoredFileRepository files) {
        this.users = users;
        this.registrationService = registrationService;
        this.resumeService = resumeService;
        this.jobService = jobService;
        this.matchService = matchService;
        this.studentProfiles = studentProfiles;
        this.companyProfiles = companyProfiles;
        this.jobs = jobs;
        this.applications = applications;
        this.bookmarks = bookmarks;
        this.files = files;
    }

    @Override
    public void run(String... args) {
        if (users.existsByEmail(MARKER_EMAIL)) {
            log.info("[demo] มีข้อมูลตัวอย่างหลักอยู่แล้ว ข้ามการสร้าง");
        } else {
            try {
                load();
                log.info("[demo] สร้างข้อมูลตัวอย่างเสร็จ: นักศึกษา {} คน บริษัท {} แห่ง ประกาศงาน {} ตำแหน่ง",
                        STUDENTS.size(), COMPANIES.size(), JOBS.size());
            } catch (RuntimeException e) {
                // เช่น service NLP ยังไม่รัน: แอปยังเปิดได้ แค่ไม่มีข้อมูลตัวอย่าง
                log.warn("[demo] สร้างข้อมูลตัวอย่างไม่สำเร็จ (เปิด service NLP ก่อนแล้วรันใหม่): {}", e.getMessage());
                return;
            }
        }
        try {
            loadExtras();
        } catch (RuntimeException e) {
            log.warn("[demo] เติมข้อมูลตัวอย่างส่วนเสริมไม่สำเร็จ: {}", e.getMessage());
        }
    }

    /** สร้างข้อมูลหลักทั้งหมด (แยกออกมาเพื่อให้ทดสอบได้) */
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

    /**
     * ส่วนเสริมตามเอกสารบทที่ 3: บัญชีผู้ดูแลระบบ, ข้อมูลนักศึกษา/บริษัท, ข้อมูลเพิ่มเติมของประกาศ,
     * คำขอยืนยันตัวตน, ใบสมัครหลายสถานะ และงานที่บันทึกไว้ ทุกขั้นข้ามถ้ามีอยู่แล้ว (รันซ้ำได้)
     */
    public void loadExtras() {
        if (!users.existsByEmail(ADMIN_EMAIL)) {
            registrationService.createAdmin("ผู้ดูแลระบบ (ตัวอย่าง)", ADMIN_EMAIL, DEMO_PASSWORD);
        }
        for (int i = 0; i < STUDENTS.size(); i++) {
            int idx = i;
            users.findByEmail(STUDENTS.get(i).email()).ifPresent(u -> {
                if (studentProfiles.findByUserId(u.getId()).isPresent()) return;
                String[] info = STUDENT_INFO.get(idx);
                String[] name = u.getDisplayName().split("\\s+", 2);
                StudentProfile p = new StudentProfile();
                p.setUser(u);
                p.setFirstName(name[0]);
                p.setLastName(name.length > 1 ? name[1] : "");
                p.setStudentId(info[0]);
                p.setFaculty(info[1]);
                p.setMajor(info[2]);
                studentProfiles.save(p);
            });
        }
        Optional<User> c1 = users.findByEmail(COMPANIES.get(0).email());
        Optional<User> c2 = users.findByEmail(COMPANIES.get(1).email());
        c1.ifPresent(u -> companyProfile(u, "0105500000001", "99/9 ถนนตัวอย่าง แขวงตัวอย่าง เขตตัวอย่าง กรุงเทพมหานคร 10000 (ที่อยู่สมมติ)",
                "ไอที / เทคโนโลยีสารสนเทศ", "120", "สัมภาษณ์ออนไลน์", VerificationStatus.APPROVED));
        c2.ifPresent(u -> companyProfile(u, "0105500000002", "1 หมู่ 1 ตำบลแม่กา อำเภอเมือง จังหวัดพะเยา 56000 (ที่อยู่สมมติ)",
                "ซอฟต์แวร์และดิจิทัลเอเจนซี", "40", "ทดสอบปฏิบัติ + สัมภาษณ์", VerificationStatus.PENDING));

        c1.ifPresent(u -> jobs.findByCompanyId(u.getId()).forEach(j -> jobExtras(j,
                "WFH 300 บาท/วัน, Onsite 400 บาท/วัน", "5 วัน / สัปดาห์ (จันทร์ - ศุกร์)", "ไม่มีสวัสดิการที่พัก", "ไม่มีรถรับส่งพนักงาน")));
        c2.ifPresent(u -> jobs.findByCompanyId(u.getId()).forEach(j -> jobExtras(j,
                "เบี้ยเลี้ยง 6,000 บาท/เดือน", "5 วัน / สัปดาห์ (จันทร์ - ศุกร์) 09:00-17:00", "มีหอพักให้ (สมมติ)", "ไม่มีรถรับส่งพนักงาน")));

        Optional<User> s1 = users.findByEmail(STUDENTS.get(0).email());
        Optional<User> s2 = users.findByEmail(STUDENTS.get(1).email());
        Optional<User> s3 = users.findByEmail(STUDENTS.get(2).email());
        apply(s1, "Backend Developer", ApplicationStatus.PENDING,
                "สวัสดีครับ ผมสนใจตำแหน่ง Backend Developer เนื่องจากมีประสบการณ์พัฒนา REST API ด้วย Java และ Spring Boot "
                        + "อยากได้โอกาสเรียนรู้การทำงานจริงร่วมกับทีมครับ (ข้อความตัวอย่าง)");
        apply(s2, "Backend Developer", ApplicationStatus.REVIEWED, "สนใจเรียนรู้งานหลังบ้านเพิ่มเติมค่ะ (ข้อความตัวอย่าง)");
        apply(s2, "Frontend Developer", ApplicationStatus.REVIEWED, "ถนัดงานหน้าเว็บด้วย React และ Figma ค่ะ (ข้อความตัวอย่าง)");
        apply(s3, "Data Analyst", ApplicationStatus.ACCEPTED, "สนใจงานวิเคราะห์ข้อมูลด้วย Python และ SQL ครับ (ข้อความตัวอย่าง)");
        apply(s3, "Backend Developer", ApplicationStatus.REJECTED, null);
        apply(s1, "DevOps / QA", ApplicationStatus.PENDING, null);
        bookmark(s1, "Frontend Developer");
        bookmark(s1, "Mobile App Developer");
    }

    private void companyProfile(User u, String taxId, String address, String type, String employees, String selection,
                                VerificationStatus status) {
        if (companyProfiles.findByUserId(u.getId()).isPresent()) return;
        CompanyProfile p = new CompanyProfile();
        p.setUser(u);
        p.setContactFirstName("ผู้ติดต่อ");
        p.setContactLastName("(ตัวอย่าง)");
        p.setTaxId(taxId);
        p.setAddress(address);
        p.setBusinessType(type);
        p.setEmployeeCount(employees);
        p.setSelectionMethod(selection);
        StoredFile doc = new StoredFile();
        doc.setOwner(u);
        doc.setFileName("DBD_Certificate_DEMO.pdf");
        doc.setContentType("application/pdf");
        byte[] pdf = demoPdf(u.getDisplayName().contains("เทคโนโลยี") ? "Demo company 1" : "Demo company 2", taxId);
        doc.setData(pdf);
        doc.setSizeBytes(pdf.length);
        p.setDocumentFileId(files.save(doc).getId());
        p.setVerificationStatus(status);
        p.setSubmittedAt(LocalDateTime.now().minusDays(status == VerificationStatus.APPROVED ? 10 : 1));
        if (status == VerificationStatus.APPROVED) {
            p.setReviewedAt(LocalDateTime.now().minusDays(9));
            p.setReviewedBy("ผู้ดูแลระบบ (ตัวอย่าง)");
        }
        companyProfiles.save(p);
    }

    private void jobExtras(Job j, String compensation, String schedule, String accommodation, String transport) {
        if (j.getCompensation() != null) return;
        j.setCompensation(compensation);
        j.setWorkSchedule(schedule);
        j.setAccommodation(accommodation);
        j.setTransport(transport);
        jobs.save(j);
    }

    private void apply(Optional<User> student, String titlePrefix, ApplicationStatus status, String coverLetter) {
        student.ifPresent(s -> findJob(titlePrefix).ifPresent(job -> {
            if (applications.existsByStudentIdAndJobId(s.getId(), job.getId())) return;
            JobApplication a = new JobApplication();
            a.setStudent(s);
            a.setJob(job);
            a.setCoverLetter(coverLetter);
            a.setStatus(status);
            applications.save(a);
        }));
    }

    private void bookmark(Optional<User> student, String titlePrefix) {
        student.ifPresent(s -> findJob(titlePrefix).ifPresent(job -> {
            if (bookmarks.existsByStudentIdAndJobId(s.getId(), job.getId())) return;
            Bookmark b = new Bookmark();
            b.setStudent(s);
            b.setJob(job);
            bookmarks.save(b);
        }));
    }

    private Optional<Job> findJob(String titlePrefix) {
        return jobs.findAll().stream().filter(j -> j.getTitle().startsWith(titlePrefix)).findFirst();
    }

    /** PDF ตัวอย่างหนึ่งหน้า (ภาษาอังกฤษ) แทนเอกสาร ภ.พ.20 จริง ใช้สาธิตหน้าอนุมัติสถานประกอบการ */
    private static byte[] demoPdf(String company, String taxId) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
                cs.newLineAtOffset(72, 700);
                cs.showText("DEMO ONLY - not a real VAT (Phor Phor 20) certificate");
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 13);
                cs.newLineAtOffset(0, -32);
                cs.showText("Company: " + company + "   Tax ID: " + taxId);
                cs.newLineAtOffset(0, -22);
                cs.showText("Generated by Workmatch demo data for the admin verification screen.");
                cs.endText();
            }
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("สร้าง PDF ตัวอย่างไม่ได้", e);
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
