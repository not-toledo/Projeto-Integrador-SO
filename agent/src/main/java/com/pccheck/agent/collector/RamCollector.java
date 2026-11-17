package com.pccheck.agent.collector;

import com.pccheck.agent.model.MemoryInfo;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;

/** Coleta total, disponível, usada e percentual de utilização da memória RAM. */
public class RamCollector implements Collector<MemoryInfo> {

    private final SystemInfo systemInfo;

    public RamCollector(SystemInfo systemInfo) {
        this.systemInfo = systemInfo;
    }

    @Override
    public String name() {
        return "RamCollector";
    }

    @Override
    public MemoryInfo collect() {
        GlobalMemory memory = systemInfo.getHardware().getMemory();
        Long total = Metrics.positiveOrNull(memory.getTotal());
        Long available = memory.getAvailable() >= 0 ? memory.getAvailable() : null;
        Long used = (total != null && available != null && available <= total) ? total - available : null;
        return new MemoryInfo(total, available, used, Metrics.percent(used, total));
    }
}
