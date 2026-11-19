package com.pccheck.agent.collector;

import com.pccheck.agent.model.SensorInfo;
import com.pccheck.agent.sensors.SensorReading;
import com.pccheck.agent.sensors.SensorSource;
import com.pccheck.agent.sensors.SensorsUnavailableException;
import oshi.SystemInfo;

/**
 * Lê a temperatura da CPU. A fonte preferida é o leitor de sensores do NEXCORE (sensor-host), que lê o
 * sensor real da CPU; sem ele, tenta o que o sistema operacional expõe via OSHI (útil no Linux; no
 * Windows quase sempre sem leitura). Sem nenhuma das fontes o resultado é "não disponível", com o
 * motivo, nunca um valor estimado.
 */
public class SensorCollector implements Collector<SensorInfo> {

    private final SystemInfo systemInfo;
    private final SensorSource sensors;

    public SensorCollector(SystemInfo systemInfo, SensorSource sensors) {
        this.systemInfo = systemInfo;
        this.sensors = sensors;
    }

    @Override
    public String name() {
        return "SensorCollector";
    }

    @Override
    public SensorInfo collect() throws CollectorUnavailableException {
        Double cpuTemp = null;
        String reason;
        try {
            SensorReading reading = sensors.read();
            Double value = reading.cpuTemperatureCelsius();
            cpuTemp = value == null ? null : Metrics.validTemperature(value);
            reason = reading.limitation();
        } catch (SensorsUnavailableException e) {
            reason = e.getMessage();
        }
        if (cpuTemp == null) {
            cpuTemp = Metrics.validTemperature(systemInfo.getHardware().getSensors().getCpuTemperature());
        }
        if (cpuTemp == null) {
            throw new CollectorUnavailableException(reason != null ? reason
                    : "O hardware não informou a temperatura da CPU.");
        }
        return new SensorInfo(cpuTemp);
    }
}
