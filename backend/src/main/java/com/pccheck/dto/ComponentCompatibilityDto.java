package com.pccheck.dto;

/**
 * Resultado de um componente.
 *
 * @param benchmark comparação por benchmark usada para CPU/GPU de modelos diferentes; null quando não foi usada
 */
public record ComponentCompatibilityDto(String component, String requirement, String detected,
        String status, String explanation, BenchmarkDto benchmark) {

    public ComponentCompatibilityDto(String component, String requirement, String detected, String status,
                                     String explanation) {
        this(component, requirement, detected, status, explanation, null);
    }

    /**
     * Pontuações do Blender Open Data (renderização) para o modelo local e o modelo exigido, na mesma versão do
     * Blender e no mesmo backend.
     *
     * @param ratio      pontuação local ÷ pontuação exigida
     * @param confidence {@code HIGH} (10+ execuções de cada lado e dispersão baixa) ou {@code MEDIUM}
     * @param source     fonte, versão e data do snapshot
     */
    public record BenchmarkDto(String localModel, String requiredModel, double localScore, double requiredScore,
                               int localSamples, int requiredSamples, String backend, String blenderVersion,
                               double ratio, String confidence, String source) {
    }
}
