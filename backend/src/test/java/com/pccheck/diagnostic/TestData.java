package com.pccheck.diagnostic;

import com.pccheck.agent.model.CollectorResult;
import com.pccheck.agent.model.CpuInfo;
import com.pccheck.agent.model.DiskInfo;
import com.pccheck.agent.model.GpuInfo;
import com.pccheck.agent.model.MemoryInfo;
import com.pccheck.agent.model.MotherboardInfo;
import com.pccheck.agent.model.OsInfo;
import com.pccheck.agent.model.SensorInfo;
import com.pccheck.agent.model.StorageInfo;
import com.pccheck.agent.model.SystemSnapshot;
import com.pccheck.agent.model.VolumeInfo;
import java.time.Instant;
import java.util.List;

/** Dados de teste construídos à mão para exercitar as regras (não são medições reais). */
final class TestData {

    static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");
    static final long GIB = 1024L * 1024L * 1024L;

    private TestData() {
    }

    static VolumeInfo volume(String mount, String fs, long total, long free) {
        return new VolumeInfo(mount, null, fs, total, free, total - free, (total - free) * 100.0 / total);
    }

    static CollectorResult<StorageInfo> storage(VolumeInfo... volumes) {
        return CollectorResult.ok("DiskCollector",
                new StorageInfo(List.of(new DiskInfo("d0", "Modelo", 100 * GIB)), List.of(volumes)));
    }

    static CollectorResult<MemoryInfo> memory(double usedPercent) {
        long total = 16 * GIB;
        long used = (long) (total * usedPercent / 100.0);
        return CollectorResult.ok("RamCollector", new MemoryInfo(total, total - used, used, usedPercent));
    }

    static CollectorResult<SensorInfo> temperature(double celsius) {
        return CollectorResult.ok("SensorCollector", new SensorInfo(celsius));
    }

    /** Snapshot com todos os coletores OK, exceto o que for substituído pelos parâmetros. */
    static SystemSnapshot snapshot(CollectorResult<MemoryInfo> memory, CollectorResult<StorageInfo> storage) {
        return new SystemSnapshot(NOW,
                CollectorResult.ok("OsCollector", new OsInfo("Windows", "11", null, "amd64", 10L)),
                CollectorResult.ok("CpuCollector", new CpuInfo("CPU X", null, 4, 8, null, null, 10.0)),
                memory, CollectorResult.ok("GpuCollector", List.of(new GpuInfo("GPU X", null, null, null))),
                CollectorResult.ok("MotherboardCollector", new MotherboardInfo("Fab", "Modelo", null, null, null, null)),
                storage);
    }

    static HealthInput input(CollectorResult<MemoryInfo> memory, CollectorResult<StorageInfo> storage,
                             CollectorResult<SensorInfo> sensors) {
        return new HealthInput(snapshot(memory, storage), sensors);
    }
}
