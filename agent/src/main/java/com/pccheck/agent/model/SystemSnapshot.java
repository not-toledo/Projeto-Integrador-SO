package com.pccheck.agent.model;

import java.time.Instant;
import java.util.List;

/** Resultado de uma coleta completa, com um resultado independente por coletor. */
public record SystemSnapshot(Instant collectedAt,
                             CollectorResult<OsInfo> os,
                             CollectorResult<CpuInfo> cpu,
                             CollectorResult<MemoryInfo> memory,
                             CollectorResult<List<GpuInfo>> gpu,
                             CollectorResult<MotherboardInfo> motherboard,
                             CollectorResult<StorageInfo> storage) {
}
