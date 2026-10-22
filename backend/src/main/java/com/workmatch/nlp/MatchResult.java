package com.workmatch.nlp;

import java.util.List;

/** ผลจาก /match: {"score":50.0,"cosine":70.7,"matched":["Java"],"missing":["Git"]} */
public record MatchResult(double score, double cosine, List<String> matched, List<String> missing) {
}
