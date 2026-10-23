package com.workmatch.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * ไม่พบประกาศงาน หรือเป็นประกาศของบริษัทอื่น -> ตอบ 404 และแสดง templates/error/404.html (ภาษาไทย, งาน A5)
 * TODO(integration, ตกลงร่วมกับชญาณัตถพล): ส่วน th:if="${job == null}" ใน job-detail.html ยังไม่ถูกใช้
 *   ถ้าอยากให้แสดงหน้านั้นแทน error/404.html ให้ controller render job-detail พร้อม status 404 และ job = null
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class JobNotFoundException extends RuntimeException {
    public JobNotFoundException() {
        super("ไม่พบตำแหน่งงาน");
    }
}
