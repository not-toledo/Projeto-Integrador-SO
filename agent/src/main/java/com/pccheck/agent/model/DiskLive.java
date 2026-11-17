package com.pccheck.agent.model;

import java.util.List;

/** Discos em tempo real. Cada lista é independente: nula = não disponível. */
public record DiskLive(List<DiskActivity> drives, List<VolumeInfo> volumes) {

    public DiskLive {
        drives = drives == null ? null : List.copyOf(drives);
        volumes = volumes == null ? null : List.copyOf(volumes);
    }
}
