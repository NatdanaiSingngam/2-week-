package com.workmatch.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * ไม่พบประกาศงาน หรือเป็นประกาศของบริษัทอื่น -> 404
 * หน้า /company/jobs/{id} จับไว้แล้วแสดง job-detail ส่วน "ไม่พบตำแหน่งงาน" (JobController.detail)
 * ที่อื่นที่หลุดมาจะใช้ templates/error/404.html
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class JobNotFoundException extends RuntimeException {
    public JobNotFoundException() {
        super("ไม่พบตำแหน่งงาน");
    }
}
