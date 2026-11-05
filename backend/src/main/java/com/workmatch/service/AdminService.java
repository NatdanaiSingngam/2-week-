package com.workmatch.service;

import com.workmatch.dto.Views.AdminOverview;
import com.workmatch.dto.Views.CompanyVerificationItem;
import com.workmatch.dto.Views.MonthCount;
import com.workmatch.dto.Views.UserItem;
import com.workmatch.entity.ApplicationStatus;
import com.workmatch.entity.CompanyProfile;
import com.workmatch.entity.Role;
import com.workmatch.entity.User;
import com.workmatch.entity.VerificationStatus;
import com.workmatch.nlp.NlpClient;
import com.workmatch.repository.CompanyProfileRepository;
import com.workmatch.repository.JobApplicationRepository;
import com.workmatch.repository.JobRepository;
import com.workmatch.repository.MatchRepository;
import com.workmatch.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** งานของผู้ดูแลระบบ (เอกสาร 1.4.3): ภาพรวม (รูปที่ 48), อนุมัติสถานประกอบการ (รูปที่ 49), จัดการบัญชีผู้ใช้ */
@Service
public class AdminService {

    private static final Locale TH = Locale.forLanguageTag("th-TH");

    private final UserRepository users;
    private final CompanyProfileRepository companyProfiles;
    private final JobRepository jobs;
    private final JobApplicationRepository applications;
    private final MatchRepository matches;
    private final NlpClient nlpClient;
    private final ErrorLogService errorLogs;

    public AdminService(UserRepository users, CompanyProfileRepository companyProfiles, JobRepository jobs,
                        JobApplicationRepository applications, MatchRepository matches, NlpClient nlpClient,
                        ErrorLogService errorLogs) {
        this.users = users;
        this.companyProfiles = companyProfiles;
        this.jobs = jobs;
        this.applications = applications;
        this.matches = matches;
        this.nlpClient = nlpClient;
        this.errorLogs = errorLogs;
    }

    @Transactional(readOnly = true)
    public AdminOverview overview() {
        // จำนวนใบสมัครรายเดือน 6 เดือนล่าสุด (กราฟแท่งในหน้า System Overview)
        YearMonth now = YearMonth.now();
        YearMonth from = now.minusMonths(5);
        Map<YearMonth, Long> byMonth = applications.findAppliedSince(from.atDay(1).atStartOfDay()).stream()
                .collect(Collectors.groupingBy(YearMonth::from, Collectors.counting()));
        long max = byMonth.values().stream().mapToLong(Long::longValue).max().orElse(0);
        List<MonthCount> months = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            YearMonth m = from.plusMonths(i);
            long c = byMonth.getOrDefault(m, 0L);
            String label = m.getMonth().getDisplayName(TextStyle.SHORT, TH) + " " + (m.getYear() + 543) % 100;
            months.add(new MonthCount(label, c, max == 0 ? 0 : (int) Math.round(c * 100.0 / max)));
        }
        return new AdminOverview(
                users.countByRole(Role.STUDENT),
                companyProfiles.countByVerificationStatus(VerificationStatus.APPROVED),
                jobs.countOpenJobs(),
                applications.countByStatus(ApplicationStatus.HIRED),
                users.countByRole(Role.COMPANY),
                companyProfiles.countByVerificationStatus(VerificationStatus.PENDING),
                applications.count(),
                matches.count(),
                matches.averageScore(),
                nlpClient.isHealthy(),
                errorLogs.countSince(LocalDateTime.now().minusDays(7)),
                months);
    }

    /** คำขอยืนยันตัวตนทั้งหมด (รออนุมัติขึ้นก่อน แล้วตามวันที่ส่ง) */
    @Transactional(readOnly = true)
    public List<CompanyVerificationItem> verificationRequests() {
        return companyProfiles.findSubmitted().stream()
                .sorted((a, b) -> Boolean.compare(b.getVerificationStatus() == VerificationStatus.PENDING,
                        a.getVerificationStatus() == VerificationStatus.PENDING))
                .map(p -> new CompanyVerificationItem(p.getUser().getId(), p.getUser().getDisplayName(), p.getTaxId(),
                        p.getSubmittedAt(), p.getDocumentFileId(), p.getVerificationStatus(), p.getReviewedBy(),
                        p.getReviewedAt()))
                .toList();
    }

    @Transactional
    public void review(Long companyUserId, boolean approve, User admin) {
        CompanyProfile p = companyProfiles.findByUserId(companyUserId)
                .orElseThrow(() -> new IllegalArgumentException("ไม่พบข้อมูลสถานประกอบการ"));
        if (p.getVerificationStatus() == VerificationStatus.NONE) {
            throw new IllegalArgumentException("สถานประกอบการนี้ยังไม่ได้ส่งเอกสาร");
        }
        p.setVerificationStatus(approve ? VerificationStatus.APPROVED : VerificationStatus.REJECTED);
        p.setReviewedAt(LocalDateTime.now());
        p.setReviewedBy(admin.getDisplayName());
        companyProfiles.save(p);
    }

    /** เอกสาร ภ.พ.20 ของบริษัทที่ส่งคำขอ (ใช้เปิดดูในหน้าอนุมัติ) */
    @Transactional(readOnly = true)
    public Long documentOf(Long companyUserId) {
        return companyProfiles.findByUserId(companyUserId).map(CompanyProfile::getDocumentFileId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<UserItem> usersList() {
        return users.findAllByOrderByCreatedAtDesc().stream()
                .map(u -> new UserItem(u.getId(), u.getDisplayName(), u.getEmail(), u.getRole().name(), u.isEnabled(),
                        u.getCreatedAt()))
                .toList();
    }

    /** ระงับ/เปิดใช้งานบัญชี (ระงับบัญชีผู้ดูแลระบบไม่ได้ เพื่อกันล็อกตัวเองออก) */
    @Transactional
    public boolean toggleEnabled(Long userId) {
        User u = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("ไม่พบผู้ใช้"));
        if (u.getRole() == Role.ADMIN) throw new IllegalArgumentException("ระงับบัญชีผู้ดูแลระบบไม่ได้");
        u.setEnabled(!u.isEnabled());
        users.save(u);
        return u.isEnabled();
    }

    @SuppressWarnings("unused")
    private static <T> Map<Long, T> byId(List<T> list, Function<T, Long> id) {
        return list.stream().collect(Collectors.toMap(id, Function.identity()));
    }
}
