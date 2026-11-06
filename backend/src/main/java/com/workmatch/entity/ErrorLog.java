package com.workmatch.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * ตาราง error_logs: ข้อผิดพลาดของระบบ ให้ผู้ดูแลตรวจสอบ (เอกสาร รูปที่ 52)
 * เก็บเฉพาะข้อความสรุป (ตัดความยาว) ไม่เก็บ stack trace หรือรหัสผ่าน
 */
@Entity
@Table(name = "error_logs")
public class ErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(nullable = false, length = 10)
    private String level;

    @Column(length = 30)
    private String source;

    @Column(length = 255)
    private String path;

    @Column(name = "error_type", length = 150)
    private String errorType;

    @Column(length = 1000)
    private String message;

    @Column(name = "user_email", length = 255)
    private String userEmail;

    @Column(name = "http_status")
    private Integer httpStatus;

    @PrePersist
    void onCreate() {
        if (occurredAt == null) occurredAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getErrorType() { return errorType; }
    public void setErrorType(String errorType) { this.errorType = errorType; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public Integer getHttpStatus() { return httpStatus; }
    public void setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; }
}
