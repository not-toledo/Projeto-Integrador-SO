package com.pccheck.dto;

import com.pccheck.compatibility.CompatibilityVerdict;
import java.time.Instant;
import java.util.List;

/**
 * Resultado da comparação de um jogo com o hardware local. {@code overallMinimum}/{@code overallRecommended}
 * resumem cada seção em texto; {@code verdict} traz a classificação geral (código, rótulo e motivos).
 */
public record GameCompatibilityDto(long appId, String gameName, String overallMinimum,
        String overallRecommended, List<ComponentCompatibilityDto> minimumResults,
        List<ComponentCompatibilityDto> recommendedResults, String minimumRawText,
        String recommendedRawText, Instant evaluatedAt, String source, List<String> warnings,
        CompatibilityVerdict verdict) {}
