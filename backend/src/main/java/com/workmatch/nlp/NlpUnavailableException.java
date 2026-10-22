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
