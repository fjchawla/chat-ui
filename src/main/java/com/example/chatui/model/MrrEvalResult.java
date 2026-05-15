package com.example.chatui.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MrrEvalResult(
        double mrr,
        int totalCases,
        List<CaseResult> results
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CaseResult(
            String query,
            String expectedServiceCode,
            String language,
            int rank,
            double reciprocalRank
    ) {}
}
