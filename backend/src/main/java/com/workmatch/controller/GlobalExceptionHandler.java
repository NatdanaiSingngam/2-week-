package com.workmatch.controller;

import com.workmatch.nlp.NlpUnavailableException;
import com.workmatch.service.CurrentUserService;
import com.workmatch.service.ErrorLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

/**
 * งาน A5: จับ exception ที่หลุดออกจาก controller แล้วแสดงหน้า error ภาษาไทย (templates/error.html)
 * ไม่แสดง stack trace ให้ผู้ใช้ เก็บรายละเอียดไว้ใน log และตาราง error_logs (หน้าผู้ดูแลระบบ รูปที่ 52)
 * 404 / 403 ไม่ผ่านที่นี่ แต่ Spring Boot จะใช้ templates/error/404.html และ error/403.html เอง
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ErrorLogService errorLogs;
    private final CurrentUserService currentUserService;

    public GlobalExceptionHandler(ErrorLogService errorLogs, CurrentUserService currentUserService) {
        this.errorLogs = errorLogs;
        this.currentUserService = currentUserService;
    }

    @ExceptionHandler(NlpUnavailableException.class)
    public ModelAndView nlpUnavailable(NlpUnavailableException e, HttpServletRequest request, HttpServletResponse response) {
        log.warn("service NLP ใช้งานไม่ได้: {}", e.getMessage());
        record("WARN", "NLP", request, e, HttpStatus.SERVICE_UNAVAILABLE);
        return page(response, HttpStatus.SERVICE_UNAVAILABLE, "ระบบวิเคราะห์ทักษะยังไม่พร้อม",
                NlpUnavailableException.MESSAGE);
    }

    @ExceptionHandler(DataAccessException.class)
    public ModelAndView database(DataAccessException e, HttpServletRequest request, HttpServletResponse response) {
        log.error("ฐานข้อมูลผิดพลาด", e);
        record("ERROR", "DATABASE", request, e, HttpStatus.SERVICE_UNAVAILABLE);
        return page(response, HttpStatus.SERVICE_UNAVAILABLE, "ฐานข้อมูลยังไม่พร้อม",
                "ระบบเชื่อมต่อฐานข้อมูลไม่ได้ชั่วคราว ลองใหม่อีกครั้งในอีกสักครู่");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ModelAndView tooLarge(MaxUploadSizeExceededException e, HttpServletRequest request, HttpServletResponse response) {
        record("WARN", "UPLOAD", request, e, HttpStatus.PAYLOAD_TOO_LARGE);
        return page(response, HttpStatus.PAYLOAD_TOO_LARGE, "ไฟล์ใหญ่เกินไป",
                "รองรับไฟล์ PDF ขนาดไม่เกิน 5MB กรุณาย่อขนาดไฟล์แล้วลองอีกครั้ง");
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView other(Exception e, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // exception ที่มีสถานะของตัวเอง (เช่น JobNotFoundException -> 404) ปล่อยให้ Spring จัดการตามเดิม
        if (e instanceof ErrorResponse || AnnotatedElementUtils.hasAnnotation(e.getClass(), ResponseStatus.class)) {
            throw e;
        }
        log.error("เกิดข้อผิดพลาดที่ไม่คาดคิด", e);
        record("ERROR", "APPLICATION", request, e, HttpStatus.INTERNAL_SERVER_ERROR);
        return page(response, HttpStatus.INTERNAL_SERVER_ERROR, "เกิดข้อผิดพลาดในระบบ",
                "ขออภัย ระบบทำงานผิดพลาด ลองใหม่อีกครั้ง หรือกลับไปหน้าหลัก");
    }

    private void record(String level, String source, HttpServletRequest request, Exception e, HttpStatus status) {
        String email = null;
        try {
            email = currentUserService.findCurrentUser().map(u -> u.getEmail()).orElse(null);
        } catch (RuntimeException ignored) {
            // ฐานข้อมูลล่มอยู่ก็ยังแสดงหน้า error ได้
        }
        errorLogs.record(level, source, request == null ? null : request.getMethod() + " " + request.getRequestURI(),
                e, email, status.value());
    }

    private static ModelAndView page(HttpServletResponse response, HttpStatus status, String title, String message) {
        response.setStatus(status.value());
        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(status);
        mav.addObject("status", status.value());
        mav.addObject("errorTitle", title);
        mav.addObject("errorMessage", message);
        return mav;
    }
}
