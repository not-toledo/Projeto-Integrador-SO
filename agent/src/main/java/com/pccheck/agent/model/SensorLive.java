package com.pccheck.agent.model;

import java.util.List;

/**
 * Sensores acessíveis. Campo nulo = sem leitura (nunca estimado).
 *
 * @param cpuTemperatureCelsius temperatura da CPU em °C
 * @param fanSpeedsRpm          rotação das ventoinhas em RPM (somente leituras válidas); nula = não disponível
 * @param gpuTemperatureCelsius temperatura da GPU em °C (via leitor de sensores do NEXCORE)
 */
public record SensorLive(Double cpuTemperatureCelsius, List<Integer> fanSpeedsRpm, Double gpuTemperatureCelsius) {

    public SensorLive {
        fanSpeedsRpm = fanSpeedsRpm == null ? null : List.copyOf(fanSpeedsRpm);
    }

    public SensorLive(Double cpuTemperatureCelsius, List<Integer> fanSpeedsRpm) {
        this(cpuTemperatureCelsius, fanSpeedsRpm, null);
    }
}
