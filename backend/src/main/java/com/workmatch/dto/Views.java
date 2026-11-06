package com.workmatch.dto;

import com.workmatch.entity.ApplicationStatus;
import com.workmatch.entity.JobStatus;
import com.workmatch.entity.VerificationStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ข้อมูลที่ส่งให้หน้าเว็บของฟีเจอร์ตามเอกสารบทที่ 3 (ค้นหางาน, บันทึกงาน, สมัครงาน, ผู้สมัคร, ผู้ดูแลระบบ)
 * เป็น record: ใน Thymeleaf เรียกแบบเมธอด เช่น ${item.title()}
 */
public final class Views {

    private Views() { }

    /** ข้อมูลที่บริษัทกรอกในแบบฟอร์มประกาศงาน (รูปที่ 44) */
    public record JobInput(String title, String description, String compensation, String workSchedule,
                           String accommodation, String transport, List<String> selectedSkills, boolean draft) {
        public static JobInput empty() {
            return new JobInput("", "", "", "", "", "", List.of(), false);
        }
    }

    /** หนึ่งแถวประกาศงานของบริษัท (รูปที่ 44, 47) */
    public record CompanyJobRow(Long id, String title, LocalDateTime createdAt, JobStatus status, long skillCount,
                                long matchedCount, long applicationCount) { }

    /** ภาพรวมบริษัท (รูปที่ 47) */
    public record CompanyOverview(long openJobs, long totalApplications, long pending, long hired,
                                  List<CompanyJobRow> recentJobs) { }

    /** หนึ่งรายการในหน้าค้นหางาน (รูปที่ 38) */
    public record ExploreItem(Long jobId, String title, String companyName, String location, String businessType,
                              boolean companyVerified, List<String> skills, LocalDateTime createdAt, long daysAgo,
                              Double score, boolean bookmarked) { }

    /** รายละเอียดงานสำหรับนักศึกษา (รูปที่ 39) */
    public record StudentJobView(Long jobId, String title, String description, JobStatus status, String companyName,
                                 String companyInitial, boolean companyVerified, String address, String businessType,
                                 String employeeCount, String workSchedule, String selectionMethod, String compensation,
                                 String accommodation, String transport, List<SkillView> skills, MatchView match,
                                 boolean bookmarked, ApplicationStatus applicationStatus, LocalDateTime appliedAt,
                                 boolean hasResume) { }

    /** งานที่บันทึกไว้ (รูปที่ 41) */
    public record BookmarkItem(Long jobId, String title, String companyName, LocalDateTime savedAt, boolean open,
                               boolean applied) { }

    /** ประวัติการสมัครงาน (รูปที่ 42) */
    public record StudentApplicationItem(Long applicationId, Long jobId, String jobTitle, String companyName,
                                         LocalDateTime appliedAt, ApplicationStatus status) { }

    /** ผู้สมัครหนึ่งคนในหน้าบริษัท (รูปที่ 45) */
    public record ApplicantItem(Long applicationId, Long jobId, String jobTitle, String studentName,
                                String studentInitials, String studentId, String major, List<String> matchedSkills,
                                Double score, LocalDateTime appliedAt, ApplicationStatus status) { }

    /** ทักษะที่ตำแหน่งต้องการ พร้อมบอกว่าผู้สมัครมีหรือไม่ (รูปที่ 46) */
    public record SkillCheck(String name, boolean matched) { }

    /** รายละเอียดผู้สมัคร (รูปที่ 46) */
    public record CandidateDetail(Long applicationId, Long jobId, String jobTitle, String studentName,
                                  String studentInitials, String email, String phone, String studentId, String faculty,
                                  String major, LocalDateTime appliedAt, ApplicationStatus status, String coverLetter,
                                  Double score, List<SkillCheck> skills, List<String> otherSkills, Long resumeFileId,
                                  String resumeFileName, Long resumeFileSize) { }

    /** สถิติผู้ดูแลระบบ (รูปที่ 48) */
    public record AdminOverview(long students, long approvedCompanies, long openJobs, long hired, long companies,
                                long pendingCompanies, long applications, long matches, Double averageScore,
                                boolean nlpHealthy, long errorsLast7Days, List<MonthCount> applicationsByMonth) { }

    public record MonthCount(String label, long count, int percent) { }

    /** คำขอยืนยันตัวตนสถานประกอบการ (รูปที่ 49) */
    public record CompanyVerificationItem(Long userId, String companyName, String taxId, LocalDateTime submittedAt,
                                          Long documentFileId, VerificationStatus status, String reviewedBy,
                                          LocalDateTime reviewedAt) { }

    /** บัญชีผู้ใช้สำหรับผู้ดูแล */
    public record UserItem(Long id, String name, String email, String role, boolean enabled, LocalDateTime createdAt) { }
}
