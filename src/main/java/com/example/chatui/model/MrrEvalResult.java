package com.example.chatui.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MrrEvalResult(
        double mrr,
        int totalQueries,
        int queriesWithHit,
        List<QueryResult> details
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QueryResult(
            String query,
            String expectedCode,
            int rank,               // 1-based; 0 = not found in top-K
            double reciprocalRank,
            List<String> retrievedCodes
    ) {}
}
