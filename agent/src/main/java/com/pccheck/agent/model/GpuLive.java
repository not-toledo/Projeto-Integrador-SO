package com.pccheck.agent.model;

/**
 * Telemetria de GPU em tempo real, lida do LibreHardwareMonitor (o OSHI não fornece estes dados).
 * Campo nulo = sem leitura (nunca estimado).
 *
 * @param utilizationPercent uso do núcleo da GPU, de 0 a 100
 * @param vramUsedBytes      memória de vídeo em uso
 * @param vramTotalBytes     memória de vídeo total informada pelo driver
 * @param temperatureCelsius temperatura da GPU em °C
 * @param name               nome da placa que originou as leituras
 */
public record GpuLive(Double utilizationPercent, Long vramUsedBytes, Long vramTotalBytes,
                      Double temperatureCelsius, String name) {
}
