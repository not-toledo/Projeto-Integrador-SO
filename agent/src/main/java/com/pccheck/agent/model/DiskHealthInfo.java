package com.pccheck.agent.model;

import java.util.List;

/**
 * Saúde dos discos com leitura SMART.
 *
 * @param disks      discos com pelo menos um indicador lido
 * @param limitation por que parte dos dados pode faltar (ex.: leitor sem administrador); null se nenhuma conhecida
 */
public record DiskHealthInfo(List<DiskHealth> disks, String limitation) {

    public DiskHealthInfo {
        disks = disks == null ? List.of() : List.copyOf(disks);
    }
}
