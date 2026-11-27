/*
 * ==== Workmatch · Backend · Service (ตรรกะหลักของระบบ) ====
 * ไฟล์: backend/src/main/java/com/workmatch/service/ProfileService.java
 * หน้าที่: ข้อมูลส่วนตัวนักศึกษา และข้อมูลบริษัท + อัปโหลดเอกสารยืนยันตัวตน
 * เรียกใช้: StudentProfileRepository, CompanyProfileRepository, UserRepository, FileStorageService
 * ถูกเรียกโดย: CompanyController, RegistrationService, StudentController, StudentDashboardController
 */
package com.workmatch.service;

import com.workmatch.entity.CompanyProfile;
import com.workmatch.entity.StoredFile;
import com.workmatch.entity.StudentProfile;
import com.workmatch.entity.User;
import com.workmatch.entity.VerificationStatus;
import com.workmatch.repository.CompanyProfileRepository;
import com.workmatch.repository.StudentProfileRepository;
import com.workmatch.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/** ข้อมูลส่วนตัวนักศึกษา (รูปที่ 36) และข้อมูลสถานประกอบการ + ยืนยันตัวตน (รูปที่ 43) */
@Service
public class ProfileService {

    static final Pattern STUDENT_ID = Pattern.compile("^\\d{8}$");
    static final Pattern TAX_ID = Pattern.compile("^\\d{13}$");
    static final Pattern WEBSITE = Pattern.compile("^https?://[^\\s<>\"]+$");
    static final Pattern PHONE = Pattern.compile("^[0-9+\\- ]{9,20}$");

    private final StudentProfileRepository studentProfiles;
    private final CompanyProfileRepository companyProfiles;
    private final UserRepository users;
    private final FileStorageService fileStorage;

    public ProfileService(StudentProfileRepository studentProfiles, CompanyProfileRepository companyProfiles,
                          UserRepository users, FileStorageService fileStorage) {
        this.studentProfiles = studentProfiles;
        this.companyProfiles = companyProfiles;
        this.users = users;
        this.fileStorage = fileStorage;
    }

    // ---------- นักศึกษา ----------

    /** คืนโปรไฟล์ (ยังไม่มีจะคืนแถวว่างที่ยังไม่บันทึก) */
    @Transactional(readOnly = true)
    public StudentProfile studentProfile(User student) {
        return studentProfiles.findByUserId(student.getId()).orElseGet(() -> {
            StudentProfile p = new StudentProfile();
            p.setUser(student);
            return p;
        });
    }

    /** แก้ไขข้อมูลส่วนตัว: ชื่อ-นามสกุลแสดงผลอัปเดต users.display_name ด้วย รหัสนิสิตแก้ได้ครั้งแรกครั้งเดียว */
    @Transactional
    public void updateStudent(User student, String fullName, String phone, String studentId, String faculty, String major,
                              String gpa, boolean gpaVisible, String coopStart, String coopEnd) {
        String name = trim(fullName);
        if (name.isEmpty()) throw new IllegalArgumentException("กรุณากรอกชื่อ-นามสกุล");
        if (name.length() > 150) throw new IllegalArgumentException("ชื่อยาวเกิน 150 ตัวอักษร");
        String tel = trim(phone);
        if (!tel.isEmpty() && !PHONE.matcher(tel).matches()) throw new IllegalArgumentException("รูปแบบเบอร์โทรศัพท์ไม่ถูกต้อง");

        BigDecimal grade = parseGpa(gpa);
        LocalDate start = parseDate(coopStart, "วันเริ่มสหกิจ");
        LocalDate end = parseDate(coopEnd, "วันสิ้นสุดสหกิจ");
        if (start != null && end != null && end.isBefore(start)) {
            throw new IllegalArgumentException("วันสิ้นสุดสหกิจต้องไม่ก่อนวันเริ่ม");
        }

        StudentProfile p = studentProfiles.findByUserId(student.getId()).orElseGet(() -> {
            StudentProfile n = new StudentProfile();
            n.setUser(student);
            return n;
        });
        String sid = trim(studentId);
        if ((p.getStudentId() == null || p.getStudentId().isBlank()) && !sid.isEmpty()) {
            if (!STUDENT_ID.matcher(sid).matches()) throw new IllegalArgumentException("รหัสนิสิตต้องเป็นตัวเลข 8 หลัก");
            p.setStudentId(sid);
        }
        String[] parts = name.split("\\s+", 2);
        p.setFirstName(parts[0]);
        p.setLastName(parts.length > 1 ? parts[1] : "");
        p.setPhone(blankToNull(tel));
        p.setFaculty(blankToNull(limit(trim(faculty), 150)));
        // สาขาต้องเลือกจากรายการมาตรฐาน (Majors) ยกเว้นค่าเดิมที่เคยพิมพ์ไว้ก่อนเปลี่ยนเป็น dropdown ให้คงไว้ได้
        String m = trim(major);
        if (!m.isEmpty() && !Majors.isStandard(m) && !m.equals(p.getMajor())) {
            throw new IllegalArgumentException("กรุณาเลือกสาขาวิชาจากรายการ");
        }
        p.setMajor(blankToNull(limit(m, 150)));
        p.setGpa(grade);
        p.setGpaVisible(grade != null && gpaVisible);
        // หน้าโปรไฟล์ไม่มีช่องวันสหกิจแล้ว: ถ้าไม่ได้ส่งค่ามา (null) ให้คงค่าเดิมไว้ ไม่ล้างทิ้ง
        if (coopStart != null) p.setCoopStart(start);
        if (coopEnd != null) p.setCoopEnd(end);
        studentProfiles.save(p);

        User u = users.findById(student.getId()).orElseThrow();
        u.setDisplayName(name);
        users.save(u);
    }

    // ---------- สถานประกอบการ ----------

    @Transactional(readOnly = true)
    public CompanyProfile companyProfile(User company) {
        return companyProfiles.findByUserId(company.getId()).orElseGet(() -> {
            CompanyProfile p = new CompanyProfile();
            p.setUser(company);
            return p;
        });
    }

    /**
     * บันทึกข้อมูลสถานประกอบการ ถ้าแนบเอกสาร PDF จะส่งคำขอยืนยันตัวตนใหม่ (สถานะ "รออนุมัติ")
     * เปลี่ยนเลขประจำตัวผู้เสียภาษีหลังอนุมัติแล้ว ต้องส่งตรวจใหม่
     */
    @Transactional
    public void updateCompany(User company, String companyName, String taxId, String address, String businessType,
                              String employeeCount, String selectionMethod, String about, String website,
                              MultipartFile document) {
        String name = trim(companyName);
        if (name.isEmpty()) throw new IllegalArgumentException("กรุณากรอกชื่อสถานประกอบการ");
        if (name.length() > 150) throw new IllegalArgumentException("ชื่อสถานประกอบการยาวเกิน 150 ตัวอักษร");
        String tax = trim(taxId);
        if (!tax.isEmpty() && !TAX_ID.matcher(tax).matches()) {
            throw new IllegalArgumentException("เลขประจำตัวผู้เสียภาษีต้องเป็นตัวเลข 13 หลัก");
        }
        String site = trim(website);
        if (site.length() > 255 || (!site.isEmpty() && !WEBSITE.matcher(site).matches())) {
            throw new IllegalArgumentException("เว็บไซต์ต้องขึ้นต้นด้วย http:// หรือ https://");
        }
        CompanyProfile p = companyProfiles.findByUserId(company.getId()).orElseGet(() -> {
            CompanyProfile n = new CompanyProfile();
            n.setUser(company);
            return n;
        });
        boolean taxChanged = p.getTaxId() != null && !p.getTaxId().equals(tax);
        p.setTaxId(blankToNull(tax));
        p.setAddress(blankToNull(limit(trim(address), 2000)));
        p.setBusinessType(blankToNull(limit(trim(businessType), 150)));
        p.setEmployeeCount(blankToNull(limit(trim(employeeCount), 50)));
        p.setSelectionMethod(blankToNull(limit(trim(selectionMethod), 200)));
        p.setAbout(blankToNull(limit(trim(about), 2000)));
        p.setWebsite(blankToNull(site));

        boolean hasUpload = document != null && !document.isEmpty();
        if (hasUpload) {
            if (p.getTaxId() == null) throw new IllegalArgumentException("กรุณากรอกเลขประจำตัวผู้เสียภาษีก่อนส่งเอกสาร");
            StoredFile saved = fileStorage.storePdf(company, document);
            Long old = p.getDocumentFileId();
            p.setDocumentFileId(saved.getId());
            fileStorage.delete(old);
            markPending(p);
        } else if (taxChanged && p.getVerificationStatus() == VerificationStatus.APPROVED) {
            markPending(p);
        }
        companyProfiles.save(p);

        User u = users.findById(company.getId()).orElseThrow();
        u.setDisplayName(name);
        users.save(u);
    }

    private static void markPending(CompanyProfile p) {
        p.setVerificationStatus(VerificationStatus.PENDING);
        p.setSubmittedAt(LocalDateTime.now());
        p.setReviewedAt(null);
        p.setReviewedBy(null);
    }

    static LocalDate parseDate(String value, String label) {
        String v = trim(value);
        if (v.isEmpty()) return null;
        LocalDate d;
        try {
            d = LocalDate.parse(v);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(label + "ไม่ถูกต้อง");
        }
        if (d.getYear() < 2000 || d.getYear() > 2100) throw new IllegalArgumentException(label + "ไม่ถูกต้อง (ใช้ปี ค.ศ.)");
        return d;
    }

    private static BigDecimal parseGpa(String value) {
        String v = trim(value);
        if (v.isEmpty()) return null;
        if (!v.matches("[0-4](\\.[0-9]{1,2})?") || new BigDecimal(v).compareTo(new BigDecimal("4")) > 0) {
            throw new IllegalArgumentException("GPA ต้องอยู่ระหว่าง 0.00-4.00 (ทศนิยมไม่เกิน 2 ตำแหน่ง)");
        }
        return new BigDecimal(v).setScale(2);
    }

    static String trim(String s) { return s == null ? "" : s.trim(); }
    static String blankToNull(String s) { return s == null || s.isBlank() ? null : s; }
    static String limit(String s, int max) { return s.length() > max ? s.substring(0, max) : s; }
}
