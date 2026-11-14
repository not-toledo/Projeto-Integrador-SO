package com.pccheck.diagnostic;

import java.time.Instant;

/**
 * Alerta explicável do Health Check.
 *
 * @param id            identificador estável (ex.: "disk-space:C:\")
 * @param component     componente avaliado (ex.: "Temperatura", "Disco", "Memória RAM")
 * @param severity      Informação, Atenção ou Crítico
 * @param kind          natureza do alerta (normal, preventivo ou dado indisponível)
 * @param title         resumo curto
 * @param description   explicação em linguagem simples, incluindo o que o alerta NÃO significa
 * @param observedValue valor observado já formatado com unidade (nulo se não houve leitura)
 * @param threshold     limite aplicado, com unidade (nulo se não se aplica)
 * @param timestamp     horário da avaliação
 */
public record Alert(String id, String component, Severity severity, AlertKind kind, String title,
                    String description, String observedValue, String threshold, Instant timestamp) {
}
