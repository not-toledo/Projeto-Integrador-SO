package com.pccheck.agent.collector;

import com.pccheck.agent.model.SensorInfo;
import oshi.SystemInfo;

/**
 * Lê sensores de temperatura expostos pelo sistema operacional via OSHI.
 * Muitos computadores (principalmente no Windows, sem driver/privilégio específico) não expõem
 * a temperatura; nesse caso o resultado é "não disponível", nunca um valor estimado.
 */
public class SensorCollector implements Collector<SensorInfo> {

    private final SystemInfo systemInfo;

    public SensorCollector(SystemInfo systemInfo) {
        this.systemInfo = systemInfo;
    }

    @Override
    public String name() {
        return "SensorCollector";
    }

    @Override
    public SensorInfo collect() {
        Double cpuTemp = Metrics.validTemperature(systemInfo.getHardware().getSensors().getCpuTemperature());
        return cpuTemp == null ? null : new SensorInfo(cpuTemp);
    }
}
