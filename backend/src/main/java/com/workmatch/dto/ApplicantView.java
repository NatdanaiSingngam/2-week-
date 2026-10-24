package com.workmatch.dto;

import java.util.List;

/** หนึ่งแถวผู้สมัครในหน้า company/job-detail: {studentName, score, cosine, matched, missing} (ไม่มีอีเมล) */
public class ApplicantView {
    private final String studentName;
    private final Double score;
    private final Double cosine;
    private final List<String> matched;
    private final List<String> missing;

    public ApplicantView(String studentName, Double score, Double cosine, List<String> matched, List<String> missing) {
        this.studentName = studentName;
        this.score = score;
        this.cosine = cosine;
        this.matched = matched;
        this.missing = missing;
    }

    public String getStudentName() { return studentName; }
    public Double getScore() { return score; }
    public Double getCosine() { return cosine; }
    public List<String> getMatched() { return matched; }
    public List<String> getMissing() { return missing; }
}
