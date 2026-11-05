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

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/** ข้อมูลส่วนตัวนักศึกษา (รูปที่ 36) และข้อมูลสถานประกอบการ + ยืนยันตัวตน (รูปที่ 43) */
@Service
public class ProfileService {

    static final Pattern STUDENT_ID = Pattern.compile("^\\d{8}$");
    static final Pattern TAX_ID = Pattern.compile("^\\d{13}$");
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
    public void updateStudent(User student, String fullName, String phone, String studentId, String faculty, String major) {
        String name = trim(fullName);
        if (name.isEmpty()) throw new IllegalArgumentException("กรุณากรอกชื่อ-นามสกุล");
        if (name.length() > 150) throw new IllegalArgumentException("ชื่อยาวเกิน 150 ตัวอักษร");
        String tel = trim(phone);
        if (!tel.isEmpty() && !PHONE.matcher(tel).matches()) throw new IllegalArgumentException("รูปแบบเบอร์โทรศัพท์ไม่ถูกต้อง");

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
        p.setMajor(blankToNull(limit(trim(major), 150)));
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
                              String employeeCount, String selectionMethod, MultipartFile document) {
        String name = trim(companyName);
        if (name.isEmpty()) throw new IllegalArgumentException("กรุณากรอกชื่อสถานประกอบการ");
        if (name.length() > 150) throw new IllegalArgumentException("ชื่อสถานประกอบการยาวเกิน 150 ตัวอักษร");
        String tax = trim(taxId);
        if (!tax.isEmpty() && !TAX_ID.matcher(tax).matches()) {
            throw new IllegalArgumentException("เลขประจำตัวผู้เสียภาษีต้องเป็นตัวเลข 13 หลัก");
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

    static String trim(String s) { return s == null ? "" : s.trim(); }
    static String blankToNull(String s) { return s == null || s.isBlank() ? null : s; }
    static String limit(String s, int max) { return s.length() > max ? s.substring(0, max) : s; }
}
