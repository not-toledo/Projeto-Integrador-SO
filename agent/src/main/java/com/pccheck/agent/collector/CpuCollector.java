package com.pccheck.agent.collector;

import com.pccheck.agent.model.CpuInfo;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

/** Coleta identificação, núcleos, frequências e uso atual do processador. */
public class CpuCollector implements Collector<CpuInfo> {

    /** Intervalo de medição do uso de CPU, em milissegundos. */
    static final long USAGE_SAMPLE_MS = 500;

    private final SystemInfo systemInfo;

    public CpuCollector(SystemInfo systemInfo) {
        this.systemInfo = systemInfo;
    }

    @Override
    public String name() {
        return "CpuCollector";
    }

    @Override
    public CpuInfo collect() {
        CentralProcessor processor = systemInfo.getHardware().getProcessor();
        CentralProcessor.ProcessorIdentifier id = processor.getProcessorIdentifier();
        return new CpuInfo(
                Metrics.cleanText(id.getName()),
                Metrics.cleanText(id.getVendor()),
                Metrics.positiveOrNull(processor.getPhysicalProcessorCount()),
                Metrics.positiveOrNull(processor.getLogicalProcessorCount()),
                Metrics.positiveOrNull(id.getVendorFreq()),
                Metrics.positiveOrNull(processor.getMaxFreq()),
                readUsage(processor));
    }

    /** Falha apenas na leitura de uso não impede a identificação do processador. */
    private static Double readUsage(CentralProcessor processor) {
        try {
            return Metrics.cpuLoadToPercent(processor.getSystemCpuLoad(USAGE_SAMPLE_MS));
        } catch (RuntimeException e) {
            return null;
        }
    }
}
