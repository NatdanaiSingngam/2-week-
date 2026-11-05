package com.workmatch.service;

import com.workmatch.entity.ErrorLog;
import com.workmatch.repository.ErrorLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * บันทึกข้อผิดพลาดลงตาราง error_logs ให้ผู้ดูแลระบบดู (เอกสาร รูปที่ 52)
 * บันทึกใน transaction แยก และไม่โยน exception ต่อ (ถ้าฐานข้อมูลล่มก็มีแค่ใน log ไฟล์)
 */
@Service
public class ErrorLogService {

    private static final Logger log = LoggerFactory.getLogger(ErrorLogService.class);

    private final ErrorLogRepository logs;

    public ErrorLogService(ErrorLogRepository logs) {
        this.logs = logs;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String level, String source, String path, Throwable error, String userEmail, Integer httpStatus) {
        try {
            ErrorLog e = new ErrorLog();
            e.setLevel(level);
            e.setSource(source);
            e.setPath(cut(path, 255));
            e.setErrorType(error == null ? null : cut(error.getClass().getSimpleName(), 150));
            e.setMessage(error == null ? null : cut(String.valueOf(error.getMessage()), 1000));
            e.setUserEmail(cut(userEmail, 255));
            e.setHttpStatus(httpStatus);
            logs.save(e);
        } catch (RuntimeException ex) {
            log.warn("บันทึก error log ลงฐานข้อมูลไม่สำเร็จ: {}", ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<ErrorLog> latest() {
        return logs.findTop200ByOrderByOccurredAtDesc();
    }

    @Transactional(readOnly = true)
    public long countSince(LocalDateTime since) {
        return logs.findTop200ByOrderByOccurredAtDesc().stream().filter(l -> l.getOccurredAt().isAfter(since)).count();
    }

    private static String cut(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
