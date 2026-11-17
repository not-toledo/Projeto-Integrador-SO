package com.pccheck.dto;

public record ComponentCompatibilityDto(String component, String requirement, String detected,
        String status, String explanation) {}
