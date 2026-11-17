package com.pccheck.agent.model;

/**
 * Volume/partição montada. Campo nulo = não disponível.
 *
 * @param freeBytes   espaço disponível ao usuário (getUsableSpace do OSHI)
 * @param usedBytes   total menos disponível
 * @param usedPercent usedBytes sobre totalBytes (0 a 100)
 */
public record VolumeInfo(String mountPoint, String label, String fileSystem, Long totalBytes,
                         Long freeBytes, Long usedBytes, Double usedPercent) {
}
