package com.pccheck.agent.model;

/**
 * Sensores acessíveis ao sistema. Campo nulo = sem leitura (nunca estimado).
 *
 * @param cpuTemperatureCelsius temperatura da CPU em graus Celsius, somente se o sistema informou uma leitura válida
 */
public record SensorInfo(Double cpuTemperatureCelsius) {
}
