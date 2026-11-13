package com.pccheck.agent.model;

/**
 * Processador. Campo nulo = não disponível (nunca estimado pelo nome do modelo).
 *
 * @param nominalFrequencyHz frequência nominal informada pelo identificador do processador
 * @param maxFrequencyHz     frequência máxima informada pelo sistema
 * @param usagePercent       uso total (0 a 100), medido em um intervalo curto
 */
public record CpuInfo(String name, String vendor, Integer physicalCores, Integer logicalProcessors,
                      Long nominalFrequencyHz, Long maxFrequencyHz, Double usagePercent) {
}
