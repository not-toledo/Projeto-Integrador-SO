package com.pccheck.agent.model;

import java.time.Instant;

/** Resultado de uma coleta completa, com um resultado independente por coletor. */
public record SystemSnapshot(Instant collectedAt,
                             CollectorResult<OsInfo> os,
                             CollectorResult<CpuInfo> cpu,
                             CollectorResult<MemoryInfo> memory) {
}
