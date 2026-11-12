/*
 * ==== Workmatch · Backend · Controller (รับคำขอจากเบราว์เซอร์) ====
 * ไฟล์: backend/src/main/java/com/workmatch/controller/PdfResponses.java
 * หน้าที่: ตัวช่วยส่งไฟล์ PDF จากฐานข้อมูลกลับไปให้เบราว์เซอร์เปิดดู
 * ถูกเรียกโดย: AdminController, CompanyController, ResumeController
 */
package com.workmatch.controller;

import com.workmatch.entity.StoredFile;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/** ส่งไฟล์ PDF ที่เก็บไว้กลับไปให้เบราว์เซอร์เปิดดู (ไม่ให้เบราว์เซอร์เดาชนิดไฟล์เอง และห้ามฝังในเว็บอื่น) */
final class PdfResponses {

    private PdfResponses() { }

    static ResponseEntity<byte[]> inline(Optional<StoredFile> file) {
        if (file.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        StoredFile f = file.get();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(f.getFileName(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "sandbox")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(f.getData());
    }
}
