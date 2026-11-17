package com.pccheck.dto;

import java.time.Instant;
import java.util.List;

public record GameRequirementsDto(long appId, String name, String headerImage, String storeUrl,
        String minimumRawText, String recommendedRawText, List<RequirementFieldDto> minimum,
        List<RequirementFieldDto> recommended, Instant fetchedAt, String source, List<String> warnings) {}
