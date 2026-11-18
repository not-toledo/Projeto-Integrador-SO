package com.pccheck.agent.collector;

import com.pccheck.agent.model.MemoryInfo;
import com.pccheck.agent.model.VirtualMemoryInfo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import oshi.PlatformEnum;
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
            virtual = toVirtual(vm.getSwapTotal(), vm.getSwapUsed(), vm.getVirtualMax(), committedBytes(vm));
        } catch (RuntimeException e) {
            // A RAM física continua válida mesmo que o SO não informe a memória virtual.
            virtual = null;
        }
        return new MemoryInfo(total, available, used, Metrics.percent(used, total), virtual, null);
    }

    /**
     * Memória comprometida. Windows: o OSHI lê CommitTotal (GetPerformanceInfo). Linux: o OSHI devolve
     * "RAM usada + swap usado", que NÃO é memória comprometida; por isso o valor é lido de Committed_AS em
     * /proc/meminfo. Sem leitura, -1 (não disponível), nunca o valor do OSHI.
     */
    private static long committedBytes(VirtualMemory vm) {
        if (SystemInfo.getCurrentPlatform() != PlatformEnum.LINUX) {
            return vm.getVirtualInUse();
        }
        try {
            Long committed = committedAsBytes(Files.readAllLines(PROC_MEMINFO));
            return committed == null ? -1 : committed;
        } catch (IOException | RuntimeException e) {
            return -1;
        }
    }

    private static final Path PROC_MEMINFO = Path.of("/proc/meminfo");

    /** Lê "Committed_AS:   1234 kB" de /proc/meminfo (testável); null se ausente ou inválido. */
    static Long committedAsBytes(List<String> meminfo) {
        for (String line : meminfo) {
            if (!line.startsWith("Committed_AS:")) {
                continue;
            }
            String[] parts = line.substring("Committed_AS:".length()).trim().split("\\s+");
            try {
                long value = Long.parseLong(parts[0]);
                boolean kb = parts.length > 1 && parts[1].equalsIgnoreCase("kB");
                return value < 0 ? null : (kb ? value * 1024 : value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
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
