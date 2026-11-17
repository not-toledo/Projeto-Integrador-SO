package com.pccheck.agent.lhm;

import java.util.Comparator;
import java.util.List;

/**
 * Leituras extraídas do LibreHardwareMonitor em um instante. Campo nulo ou lista vazia = sem leitura.
 *
 * @param cpuTemperatureCelsius temperatura da CPU (pacote ou, na falta, a maior temperatura de núcleo)
 * @param fanSpeedsRpm          rotação das ventoinhas com leitura válida (> 0)
 */
public record LhmReading(Double cpuTemperatureCelsius, List<LhmGpu> gpus, List<Integer> fanSpeedsRpm) {

    public LhmReading {
        gpus = gpus == null ? List.of() : List.copyOf(gpus);
        fanSpeedsRpm = fanSpeedsRpm == null ? List.of() : List.copyOf(fanSpeedsRpm);
    }

    /**
     * GPU principal: a que informa mais memória de vídeo (em geral a dedicada, quando há também uma
     * integrada); em empate, a primeira da lista. Null se nenhuma GPU foi encontrada.
     */
    public LhmGpu primaryGpu() {
        return gpus.stream()
                .max(Comparator.comparingLong((LhmGpu g) -> g.vramTotalBytes() == null ? 0L : g.vramTotalBytes()))
                .orElse(null);
    }
}
