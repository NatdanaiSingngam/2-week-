/*
 * ==== Workmatch · Backend · Service (ตรรกะหลักของระบบ) ====
 * ไฟล์: backend/src/main/java/com/workmatch/service/StoredFileCleanupService.java
 * หน้าที่: ลบไฟล์ PDF ที่ค้างในตาราง stored_files (แผน 1 ธ.ค., backlog B4)
 *          ไฟล์ค้าง = อัปโหลดแล้วไม่ได้กดบันทึก (ไม่มีเรซูเม่หรือโปรไฟล์บริษัทใดอ้างถึง) และเก่ากว่า 24 ชั่วโมง
 *          ไฟล์ที่เพิ่งอัปโหลดและยังรอผู้ใช้กดบันทึกอยู่ใน session จึงไม่ถูกลบ
 * ถูกเรียกโดย: ตัวตั้งเวลา (ทุกชั่วโมง + ตอนเปิดระบบ), AdminController (POST /admin/files/cleanup)
 * เรียกใช้: StoredFileRepository.findOrphanIds()
 */
package com.workmatch.service;

import com.workmatch.repository.StoredFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class StoredFileCleanupService {

    /** อายุขั้นต่ำของไฟล์ค้างที่จะลบ: ไฟล์ที่ยังรอกดบันทึกใน session ต้องไม่ถูกลบ */
    public static final Duration MIN_AGE = Duration.ofHours(24);

    private static final Logger log = LoggerFactory.getLogger(StoredFileCleanupService.class);

    private final StoredFileRepository files;

    public StoredFileCleanupService(StoredFileRepository files) {
        this.files = files;
    }

    /** ลบไฟล์ค้างที่อัปโหลดก่อน now − minAge คืนจำนวนไฟล์ที่ลบ */
    @Transactional
    public int deleteOrphans(Duration minAge) {
        List<Long> ids = files.findOrphanIds(LocalDateTime.now().minus(minAge));
        if (!ids.isEmpty()) files.deleteAllByIdInBatch(ids);
        return ids.size();
    }

    /** ทุกชั่วโมง และตอนเปิดระบบ (ถ้าระบบปิดไปนาน ไฟล์ค้างจะไม่สะสม) */
    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT1H")
    @EventListener(ApplicationReadyEvent.class)
    public void scheduledCleanup() {
        try {
            int n = deleteOrphans(MIN_AGE);
            if (n > 0) log.info("[files] ลบไฟล์ PDF ที่ค้าง {} ไฟล์", n);
        } catch (RuntimeException e) {
            log.warn("[files] ลบไฟล์ที่ค้างไม่สำเร็จ: {}", e.getMessage());
        }
    }
}
