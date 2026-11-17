package com.pccheck.agent.lhm;

/** Leituras de uma placa de vídeo no LibreHardwareMonitor. Campo nulo = sem leitura. */
public record LhmGpu(String name, Double loadPercent, Double temperatureCelsius,
                     Long vramUsedBytes, Long vramTotalBytes) {
}
