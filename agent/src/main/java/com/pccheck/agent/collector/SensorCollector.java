package com.pccheck.agent.collector;

import com.pccheck.agent.lhm.LhmClient;
import com.pccheck.agent.lhm.LhmSource;
import com.pccheck.agent.lhm.LhmUnavailableException;
import com.pccheck.agent.model.SensorInfo;
import oshi.SystemInfo;

/**
 * Lê a temperatura da CPU. A fonte preferida é o LibreHardwareMonitor (servidor web local), que lê os
 * sensores reais da CPU; se ele não estiver rodando, tenta o que o sistema operacional expõe via OSHI
 * (no Windows, quase sempre sem leitura). Sem nenhuma das duas fontes o resultado é "não disponível",
 * nunca um valor estimado.
 */
public class SensorCollector implements Collector<SensorInfo> {

    private final SystemInfo systemInfo;
    private final LhmSource lhm;

    public SensorCollector(SystemInfo systemInfo) {
        this(systemInfo, new LhmClient(LhmClient.DEFAULT_URL));
    }

    public SensorCollector(SystemInfo systemInfo, LhmSource lhm) {
        this.systemInfo = systemInfo;
        this.lhm = lhm;
    }

    @Override
    public String name() {
        return "SensorCollector";
    }

    @Override
    public SensorInfo collect() {
        Double cpuTemp = null;
        try {
            cpuTemp = Metrics.validTemperature(valueOrNaN(lhm.read().cpuTemperatureCelsius()));
        } catch (LhmUnavailableException e) {
            // LibreHardwareMonitor fora do ar: segue para o OSHI.
        }
        if (cpuTemp == null) {
            cpuTemp = Metrics.validTemperature(systemInfo.getHardware().getSensors().getCpuTemperature());
        }
        return cpuTemp == null ? null : new SensorInfo(cpuTemp);
    }

    private static double valueOrNaN(Double value) {
        return value == null ? Double.NaN : value;
    }
}
