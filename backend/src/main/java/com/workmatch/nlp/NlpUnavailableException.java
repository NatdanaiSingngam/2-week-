/*
 * ==== Workmatch · Backend · เชื่อมต่อ NLP service (Python) ====
 * ไฟล์: backend/src/main/java/com/workmatch/nlp/NlpUnavailableException.java
 * หน้าที่: เรียก NLP service ไม่ได้หรือเกินเวลา (หน้าเว็บจะแจ้งข้อผิดพลาด และไม่บันทึกข้อมูลครึ่งๆ กลางๆ)
 * ถูกเรียกโดย: GlobalExceptionHandler, JobController, JobPostingService, NlpClient, ResumeController,
 *     ResumeService
 */
package com.workmatch.nlp;

/** เรียก service NLP ไม่ได้ หรือเกินเวลา */
public class NlpUnavailableException extends RuntimeException {
    public static final String MESSAGE = "ระบบวิเคราะห์ทักษะยังไม่พร้อม ลองใหม่อีกครั้ง";

    public NlpUnavailableException(Throwable cause) {
        super(MESSAGE, cause);
    }

    public NlpUnavailableException() {
        super(MESSAGE);
    }
}
