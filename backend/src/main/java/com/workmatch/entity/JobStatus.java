package com.workmatch.entity;

/** สถานะประกาศงาน (เอกสาร รูปที่ 44 ปุ่ม "บันทึกแบบร่าง" / "เผยแพร่ประกาศงาน") */
public enum JobStatus {
    OPEN("เปิดรับสมัคร"),
    DRAFT("แบบร่าง"),
    CLOSED("ปิดรับสมัคร");

    private final String label;

    JobStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
