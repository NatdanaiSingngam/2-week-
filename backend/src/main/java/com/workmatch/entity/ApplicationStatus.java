package com.workmatch.entity;

/** สถานะการสมัครงาน (เอกสาร รูปที่ 42, 45, 46): รอพิจารณา -> นัดสัมภาษณ์ -> รับเข้าทำงาน / ปฏิเสธ */
public enum ApplicationStatus {
    PENDING("รอพิจารณา"),
    INTERVIEW("นัดสัมภาษณ์"),
    HIRED("รับเข้าทำงาน"),
    REJECTED("ปฏิเสธ");

    private final String label;

    ApplicationStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
