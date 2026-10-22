package com.workmatch.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * ไม่พบประกาศงาน หรือเป็นประกาศของบริษัทอื่น -> ตอบ 404
 * TODO(integration, ฝั่งเรา - A5): ตอนนี้ 404 ขึ้นหน้า Whitelabel ไม่ใช่หน้า "ไม่พบตำแหน่งงาน" ใน job-detail.html
 *   (job-detail.html มีส่วน th:if="${job == null}" รองรับไว้แล้ว แต่ controller ไม่เคยส่ง job = null)
 *   ทางเลือก: ทำ templates/error/404.html หรือให้ controller ตอบ status 404 พร้อม render job-detail ที่ job = null
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class JobNotFoundException extends RuntimeException {
    public JobNotFoundException() {
        super("ไม่พบตำแหน่งงาน");
    }
}
