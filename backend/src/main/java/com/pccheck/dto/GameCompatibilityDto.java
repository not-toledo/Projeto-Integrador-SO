package com.pccheck.dto;

import java.time.Instant;
import java.util.List;

public record GameCompatibilityDto(long appId, String gameName, String overallMinimum,
        String overallRecommended, List<ComponentCompatibilityDto> minimumResults,
        List<ComponentCompatibilityDto> recommendedResults, String minimumRawText,
        String recommendedRawText, Instant evaluatedAt, String source, List<String> warnings) {}
