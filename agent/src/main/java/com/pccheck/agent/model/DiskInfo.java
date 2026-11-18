package com.pccheck.agent.model;

import java.util.List;

/**
 * Unidade de armazenamento física. Campo nulo = não disponível.
 *
 * @param name           identificador do SO (ex.: \\.\PHYSICALDRIVE0, /dev/sda)
 * @param model          modelo
 * @param sizeBytes      capacidade
 * @param mediaType      "SSD" ou "HDD", somente quando o SO informa (Windows: MSFT_PhysicalDisk.MediaType;
 *                       Linux: /sys/block/&lt;disco&gt;/queue/rotational); nunca deduzido do nome do modelo
 * @param busType        barramento informado pelo SO (ex.: "SATA", "NVMe", "USB")
 * @param mountPoints    pontos de montagem das partições deste disco (ex.: "C:\", "/home"), como o SO informa;
 *                       liga os volumes ao disco físico. Lista vazia = nenhuma partição montada ou não informado
 */
public record DiskInfo(String name, String model, Long sizeBytes, String mediaType, String busType,
                       List<String> mountPoints) {

    public DiskInfo {
        mountPoints = mountPoints == null ? List.of() : List.copyOf(mountPoints);
    }

    /** Sem pontos de montagem. */
    public DiskInfo(String name, String model, Long sizeBytes, String mediaType, String busType) {
        this(name, model, sizeBytes, mediaType, busType, List.of());
    }

    /** Sem tipo de mídia, barramento nem pontos de montagem. */
    public DiskInfo(String name, String model, Long sizeBytes) {
        this(name, model, sizeBytes, null, null, List.of());
    }
}
