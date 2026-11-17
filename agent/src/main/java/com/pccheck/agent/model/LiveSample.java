package com.pccheck.agent.model;

import java.time.Instant;

/**
 * Uma amostra do Live Monitor, com um resultado independente por grupo de métricas:
 * a falha de um grupo (por exemplo, GPU) não afeta os demais.
 *
 * @param collectedAt horário do fim da amostra
 * @param durationMs  tempo gasto para coletar a amostra
 */
public record LiveSample(Instant collectedAt, long durationMs,
                         CollectorResult<CpuLive> cpu,
                         CollectorResult<MemoryInfo> memory,
                         CollectorResult<GpuLive> gpu,
                         CollectorResult<DiskLive> disks,
                         CollectorResult<SensorLive> sensors) {
}
