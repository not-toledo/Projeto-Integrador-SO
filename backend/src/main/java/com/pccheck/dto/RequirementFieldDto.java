package com.pccheck.dto;

public record RequirementFieldDto(String component, String rawText, Long parsedBytes,
        String parsedValue, double parserConfidence, String parseStatus) {}
