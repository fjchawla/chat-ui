package com.example.chatui.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IngestionStatus(
    Long jobId,
    String sourceFile,
    String status,
    Integer chunksIngested,
    String errorMessage
) {}
