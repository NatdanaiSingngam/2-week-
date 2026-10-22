package com.workmatch.dto;

import java.util.List;

/** หนึ่งการ์ดในหน้า student/dashboard: {jobId, jobTitle, companyName, score, cosine, matched, missing} */
public class MatchView {
    private final Long jobId;
    private final String jobTitle;
    private final String companyName;
    private final Double score;
    private final Double cosine;
    private final List<String> matched;
    private final List<String> missing;

    public MatchView(Long jobId, String jobTitle, String companyName, Double score, Double cosine,
                     List<String> matched, List<String> missing) {
        this.jobId = jobId;
        this.jobTitle = jobTitle;
        this.companyName = companyName;
        this.score = score;
        this.cosine = cosine;
        this.matched = matched;
        this.missing = missing;
    }

    public Long getJobId() { return jobId; }
    public String getJobTitle() { return jobTitle; }
    public String getCompanyName() { return companyName; }
    public Double getScore() { return score; }
    public Double getCosine() { return cosine; }
    public List<String> getMatched() { return matched; }
    public List<String> getMissing() { return missing; }
}
