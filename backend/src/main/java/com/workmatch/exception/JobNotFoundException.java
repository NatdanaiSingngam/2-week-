package com.workmatch.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** ไม่พบประกาศงาน หรือเป็นประกาศของบริษัทอื่น -> ตอบ 404 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class JobNotFoundException extends RuntimeException {
    public JobNotFoundException() {
        super("ไม่พบตำแหน่งงาน");
    }
}
