package com.workmatch.controller;

import com.workmatch.nlp.NlpUnavailableException;
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
import org.springframework.web.servlet.ModelAndView;

/**
 * งาน A5: จับ exception ที่หลุดออกจาก controller แล้วแสดงหน้า error ภาษาไทย (templates/error.html)
 * ไม่แสดง stack trace ให้ผู้ใช้ เก็บรายละเอียดไว้ใน log เท่านั้น
 * 404 / 403 ไม่ผ่านที่นี่ แต่ Spring Boot จะใช้ templates/error/404.html และ error/403.html เอง
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NlpUnavailableException.class)
    public ModelAndView nlpUnavailable(NlpUnavailableException e, HttpServletResponse response) {
        log.warn("service NLP ใช้งานไม่ได้: {}", e.getMessage());
        return page(response, HttpStatus.SERVICE_UNAVAILABLE, "ระบบวิเคราะห์ทักษะยังไม่พร้อม",
                NlpUnavailableException.MESSAGE);
    }

    @ExceptionHandler(DataAccessException.class)
    public ModelAndView database(DataAccessException e, HttpServletResponse response) {
        log.error("ฐานข้อมูลผิดพลาด", e);
        return page(response, HttpStatus.SERVICE_UNAVAILABLE, "ฐานข้อมูลยังไม่พร้อม",
                "ระบบเชื่อมต่อฐานข้อมูลไม่ได้ชั่วคราว ลองใหม่อีกครั้งในอีกสักครู่");
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView other(Exception e, HttpServletResponse response) throws Exception {
        // exception ที่มีสถานะของตัวเอง (เช่น JobNotFoundException -> 404) ปล่อยให้ Spring จัดการตามเดิม
        if (e instanceof ErrorResponse || AnnotatedElementUtils.hasAnnotation(e.getClass(), ResponseStatus.class)) {
            throw e;
        }
        log.error("เกิดข้อผิดพลาดที่ไม่คาดคิด", e);
        return page(response, HttpStatus.INTERNAL_SERVER_ERROR, "เกิดข้อผิดพลาดในระบบ",
                "ขออภัย ระบบทำงานผิดพลาด ลองใหม่อีกครั้ง หรือกลับไปหน้าหลัก");
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
