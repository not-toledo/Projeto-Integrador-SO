package com.pccheck.agent.sensors;

import java.util.Comparator;
import java.util.List;

/**
 * Leituras dos sensores em um instante. Campo nulo ou lista vazia = sem leitura.
 *
 * @param cpuTemperatureCelsius temperatura da CPU (pacote ou, na falta, a maior temperatura de núcleo)
 * @param fanSpeedsRpm          rotação das ventoinhas com leitura válida (> 0)
 * @param limitation            por que parte dos sensores não pôde ser lida (ex.: sem permissão de
 *                              administrador ou sem o driver); null quando não há limitação conhecida
 */
public record SensorReading(Double cpuTemperatureCelsius, List<GpuReading> gpus, List<Integer> fanSpeedsRpm,
                            String limitation) {

    public SensorReading {
        gpus = gpus == null ? List.of() : List.copyOf(gpus);
        fanSpeedsRpm = fanSpeedsRpm == null ? List.of() : List.copyOf(fanSpeedsRpm);
    }

    /**
     * GPU principal: a que informa mais memória de vídeo (em geral a dedicada, quando há também uma
     * integrada); em empate, a primeira da lista. Null se nenhuma GPU foi encontrada.
     */
    public GpuReading primaryGpu() {
        return gpus.stream()
                .max(Comparator.comparingLong((GpuReading g) -> g.vramTotalBytes() == null
                        ? (g.vramUsedBytes() == null ? 0L : g.vramUsedBytes()) : g.vramTotalBytes()))
                .orElse(null);
    }
}
