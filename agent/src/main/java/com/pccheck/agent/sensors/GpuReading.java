package com.pccheck.agent.sensors;

/** Leituras de uma placa de vídeo. Campo nulo = sem leitura (nunca estimado). */
public record GpuReading(String name, Double loadPercent, Double temperatureCelsius,
                         Long vramUsedBytes, Long vramTotalBytes) {
}
