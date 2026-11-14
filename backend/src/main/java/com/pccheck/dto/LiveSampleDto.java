package com.pccheck.dto;

import com.pccheck.agent.model.CpuLive;
import com.pccheck.agent.model.DiskLive;
import com.pccheck.agent.model.GpuLive;
import com.pccheck.agent.model.LiveSample;
import com.pccheck.agent.model.MemoryInfo;
import com.pccheck.agent.model.SensorLive;
import java.time.Instant;

/**
 * Amostra do Live Monitor na API. Cada grupo tem estado próprio (OK, UNAVAILABLE ou ERROR);
 * campo nulo dentro de {@code data} = sem leitura.
 */
public record LiveSampleDto(Instant collectedAt,
                            long durationMs,
                            ComponentDto<CpuLive> cpu,
                            ComponentDto<MemoryInfo> memory,
                            ComponentDto<GpuLive> gpu,
                            ComponentDto<DiskLive> disks,
                            ComponentDto<SensorLive> sensors) {

    public static LiveSampleDto from(LiveSample sample) {
        return new LiveSampleDto(
                sample.collectedAt(),
                sample.durationMs(),
                ComponentDto.from(sample.cpu()),
                ComponentDto.from(sample.memory()),
                ComponentDto.from(sample.gpu()),
                ComponentDto.from(sample.disks()),
                ComponentDto.from(sample.sensors()));
    }
}
