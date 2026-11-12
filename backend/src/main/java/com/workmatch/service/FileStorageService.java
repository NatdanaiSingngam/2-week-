/*
 * ==== Workmatch · Backend · Service (ตรรกะหลักของระบบ) ====
 * ไฟล์: backend/src/main/java/com/workmatch/service/FileStorageService.java
 * หน้าที่: รับไฟล์ PDF (ตรวจว่าเป็น PDF จริง ≤ 5MB) เก็บลงฐานข้อมูล และดึงข้อความจาก PDF ด้วย PDFBox
 * เรียกใช้: StoredFileRepository
 * ถูกเรียกโดย: AdminController, CompanyController, ProfileService, ResumeController, ResumeExtrasService,
 *     StoredFile
 */
package com.workmatch.service;

import com.workmatch.entity.StoredFile;
import com.workmatch.entity.User;
import com.workmatch.repository.StoredFileRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

/**
 * เก็บไฟล์ PDF ที่อัปโหลด (เรซูเม่ / เอกสาร ภ.พ.20) ลงตาราง stored_files และอ่านข้อความจาก PDF ด้วย PDFBox
 * รับเฉพาะ PDF จริง (ตรวจหัวไฟล์ %PDF) ขนาดไม่เกิน 5MB
 */
@Service
public class FileStorageService {

    public static final long MAX_BYTES = 5L * 1024 * 1024;

    private final StoredFileRepository files;

    public FileStorageService(StoredFileRepository files) {
        this.files = files;
    }

    /** ตรวจแล้วบันทึกไฟล์ PDF คืนแถวที่บันทึก (ไม่ผ่านจะโยน IllegalArgumentException ข้อความไทย) */
    @Transactional
    public StoredFile storePdf(User owner, MultipartFile upload) {
        byte[] data = readPdf(upload);
        StoredFile f = new StoredFile();
        f.setOwner(owner);
        f.setFileName(safeName(upload.getOriginalFilename()));
        f.setContentType("application/pdf");
        f.setSizeBytes(data.length);
        f.setData(data);
        return files.save(f);
    }

    /** อ่านไบต์ของไฟล์ที่อัปโหลดพร้อมตรวจว่าเป็น PDF */
    public byte[] readPdf(MultipartFile upload) {
        if (upload == null || upload.isEmpty()) throw new IllegalArgumentException("กรุณาเลือกไฟล์ PDF");
        if (upload.getSize() > MAX_BYTES) throw new IllegalArgumentException("ไฟล์ใหญ่เกิน 5MB");
        byte[] data;
        try {
            data = upload.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("อ่านไฟล์ไม่สำเร็จ ลองอัปโหลดใหม่อีกครั้ง");
        }
        if (data.length < 5 || data[0] != '%' || data[1] != 'P' || data[2] != 'D' || data[3] != 'F') {
            throw new IllegalArgumentException("รองรับเฉพาะไฟล์ PDF");
        }
        return data;
    }

    /** ดึงข้อความจาก PDF แบบ text-based (PDF ที่เป็นภาพสแกนจะได้ข้อความว่าง) */
    public String extractText(byte[] pdf) {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            if (doc.isEncrypted()) throw new IllegalArgumentException("ไฟล์ PDF ถูกล็อกรหัสผ่าน อ่านข้อความไม่ได้");
            String text = new PDFTextStripper().getText(doc);
            return text == null ? "" : text.replace('\u0000', ' ').trim();
        } catch (IOException e) {
            throw new IllegalArgumentException("อ่านข้อความจากไฟล์ PDF ไม่ได้ ไฟล์อาจเสียหาย");
        }
    }

    @Transactional(readOnly = true)
    public Optional<StoredFile> find(Long id) {
        if (id == null) return Optional.empty();
        return files.findById(id).map(f -> {
            f.getData(); // โหลดข้อมูลไฟล์ใน transaction (คอลัมน์เป็น lazy)
            return f;
        });
    }

    @Transactional(readOnly = true)
    public Optional<String> fileName(Long id) {
        if (id == null) return Optional.empty();
        return files.findById(id).map(StoredFile::getFileName);
    }

    @Transactional
    public void delete(Long id) {
        if (id != null) files.findById(id).ifPresent(files::delete);
    }

    private static String safeName(String original) {
        String name = original == null ? "file.pdf" : original.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_").trim();
        if (name.isEmpty()) name = "file.pdf";
        if (name.length() > 200) name = name.substring(name.length() - 200);
        return name;
    }
}
