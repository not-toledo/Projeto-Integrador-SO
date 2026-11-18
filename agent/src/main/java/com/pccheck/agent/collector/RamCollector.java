package com.pccheck.agent.collector;

import com.pccheck.agent.model.MemoryInfo;
import com.pccheck.agent.model.VirtualMemoryInfo;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.hardware.VirtualMemory;

/** Coleta a memória RAM (total, disponível, usada) e a memória virtual (swap e memória comprometida). */
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
        VirtualMemoryInfo virtual;
        try {
            VirtualMemory vm = memory.getVirtualMemory();
            virtual = toVirtual(vm.getSwapTotal(), vm.getSwapUsed(), vm.getVirtualMax(), vm.getVirtualInUse());
        } catch (RuntimeException e) {
            // A RAM física continua válida mesmo que o SO não informe a memória virtual.
            virtual = null;
        }
        return new MemoryInfo(total, available, used, Metrics.percent(used, total), virtual, null);
    }

    /** Conversão pura (testável): valores negativos ou incoerentes viram null; null se nada restar. */
    static VirtualMemoryInfo toVirtual(long swapTotal, long swapUsed, long commitLimit, long commitUsed) {
        Long sTotal = swapTotal >= 0 ? swapTotal : null;
        Long sUsed = swapUsed >= 0 && (sTotal == null || swapUsed <= sTotal) ? swapUsed : null;
        Long cLimit = Metrics.positiveOrNull(commitLimit);
        Long cUsed = commitUsed >= 0 ? commitUsed : null;
        // Sem Metrics.percent: no Linux a memória comprometida pode passar do limite (overcommit).
        Double commitPercent = cLimit != null && cUsed != null ? cUsed * 100.0 / cLimit : null;
        if (sTotal == null && sUsed == null && cLimit == null && cUsed == null) {
            return null;
        }
        return new VirtualMemoryInfo(sTotal, sUsed, sTotal != null && sTotal > 0 ? Metrics.percent(sUsed, sTotal) : null,
                cLimit, cUsed, commitPercent);
    }
}
