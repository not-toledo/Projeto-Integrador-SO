package com.pccheck.agent.model;

/** Memória RAM. Campo nulo = não disponível. */
public record MemoryInfo(Long totalBytes, Long availableBytes, Long usedBytes, Double usedPercent) {
}
