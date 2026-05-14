package com.example.chatui.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatResponse(
    String serviceCode,
    boolean found,
    String description,
    String serviceNameArabic,
    String serviceNameEnglish,
    String arabicSourceExcerpt,
    String confidence,
    String additionalInfo
) {}
