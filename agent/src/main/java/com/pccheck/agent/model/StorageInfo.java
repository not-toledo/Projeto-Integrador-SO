package com.pccheck.agent.model;

import java.util.List;

/**
 * Armazenamento. Cada lista é independente: nula = não disponível (a outra pode ter dados).
 */
public record StorageInfo(List<DiskInfo> drives, List<VolumeInfo> volumes) {

    public StorageInfo {
        drives = drives == null ? null : List.copyOf(drives);
        volumes = volumes == null ? null : List.copyOf(volumes);
    }
}
