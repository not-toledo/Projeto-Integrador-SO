package com.pccheck.agent.model;

/**
 * Unidade de armazenamento física. Campo nulo = não disponível.
 * O tipo (SSD/HDD) não é informado: não há evidência confiável disponível via OSHI.
 */
public record DiskInfo(String name, String model, Long sizeBytes) {
}
