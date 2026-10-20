package com.workmatch.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** ตาราง resumes: นักศึกษา 1 คนมีเรซูเม่ 1 ฉบับ (user_id ไม่ซ้ำ) */
@Entity
@Table(name = "resumes")
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "raw_text", nullable = false, columnDefinition = "TEXT")
    private String rawText;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ตั้งเวลาอัตโนมัติทั้งตอนสร้างและตอนแก้ไข
    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
