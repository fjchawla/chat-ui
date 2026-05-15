package com.example.chatui.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MrrEvalRequest(
        int topK,
        List<TestCase> testCases
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TestCase(
            String query,
            String expectedServiceCode,
            String language
    ) {}
}
