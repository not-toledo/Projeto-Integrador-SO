package com.pccheck.diagnostic;

import java.time.Instant;
import java.util.List;

/**
 * Resultado do Health Check.
 *
 * @param collectedAt      horário da avaliação (UTC)
 * @param source           origem dos dados
 * @param overall          maior severidade entre os alertas (INFO se não houver Atenção/Crítico)
 * @param infoCount        quantidade de alertas Informação
 * @param warningCount     quantidade de alertas Atenção
 * @param criticalCount    quantidade de alertas Crítico
 * @param thresholds       limites usados na avaliação
 * @param thresholdsSource origem dos limites
 * @param alerts           alertas, do mais grave para o menos grave
 */
public record HealthReport(Instant collectedAt, String source, Severity overall, int infoCount, int warningCount,
                           int criticalCount, HealthThresholds thresholds, String thresholdsSource,
                           List<Alert> alerts) {

    public static final String SOURCE = "Avaliação de indicadores coletados localmente via OSHI; sem dados simulados";
}
