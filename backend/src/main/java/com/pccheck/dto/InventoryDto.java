package com.pccheck.dto;

import com.pccheck.agent.model.CpuInfo;
import com.pccheck.agent.model.GpuInfo;
import com.pccheck.agent.model.MemoryInfo;
import com.pccheck.agent.model.MotherboardInfo;
import com.pccheck.agent.model.OsInfo;
import com.pccheck.agent.model.StorageInfo;
import com.pccheck.agent.model.SystemSnapshot;
import java.time.Instant;
import java.util.List;

/** Inventário de hardware (Módulo A). Cada componente tem estado próprio: um falhar não afeta os demais. */
public record InventoryDto(Instant collectedAt,
                           String source,
                           ComponentDto<OsInfo> os,
                           ComponentDto<CpuInfo> cpu,
                           ComponentDto<MemoryInfo> memory,
                           ComponentDto<List<GpuInfo>> gpu,
                           ComponentDto<MotherboardInfo> motherboard,
                           ComponentDto<StorageInfo> storage) {

    public static final String SOURCE = "Coleta local via OSHI (dados lidos do proprio computador)";

    public static InventoryDto from(SystemSnapshot snapshot) {
        return new InventoryDto(
                snapshot.collectedAt(),
                SOURCE,
                ComponentDto.from(snapshot.os()),
                ComponentDto.from(snapshot.cpu()),
                ComponentDto.from(snapshot.memory()),
                ComponentDto.from(snapshot.gpu()),
                ComponentDto.from(snapshot.motherboard()),
                ComponentDto.from(snapshot.storage()));
    }
}
