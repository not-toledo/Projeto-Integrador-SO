package com.pccheck.agent.collector;

import com.pccheck.agent.model.SystemSnapshot;
import java.time.Instant;
import oshi.SystemInfo;

/** Executa todos os coletores; a falha de um não impede a leitura dos demais. */
public class SystemCollector {

    private final OsCollector os;
    private final CpuCollector cpu;
    private final RamCollector ram;
    private final GpuCollector gpu;
    private final MotherboardCollector motherboard;
    private final DiskCollector disk;

    public SystemCollector(SystemInfo systemInfo) {
        this.os = new OsCollector(systemInfo);
        this.cpu = new CpuCollector(systemInfo);
        this.ram = new RamCollector(systemInfo);
        this.gpu = new GpuCollector(systemInfo);
        this.motherboard = new MotherboardCollector(systemInfo);
        this.disk = new DiskCollector(systemInfo);
    }

    public SystemSnapshot collect() {
        return new SystemSnapshot(
                Instant.now(),
                CollectorRunner.run(os),
                CollectorRunner.run(cpu),
                CollectorRunner.run(ram),
                CollectorRunner.run(gpu),
                CollectorRunner.run(motherboard),
                CollectorRunner.run(disk));
    }
}
